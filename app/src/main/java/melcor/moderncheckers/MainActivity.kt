package melcor.moderncheckers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.BitmapFactory
import android.widget.*
import android.text.Html
import android.util.Log
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.bg
import java.util.stream.Collectors
import java.util.stream.Stream
import android.animation.ObjectAnimator
import android.graphics.Path
import android.view.*
import kotlinx.coroutines.experimental.delay
import melcor.neurocheckers.network.*


private val fieldSize = 112

class AndroidChecker(context: Context, val color: Int) {
    val view: ImageView
    private val checkerSize = (fieldSize *0.8).toInt()
    lateinit var position: Position
    var isQueen = false
        private set

    init {
        view = ImageView(context).apply {
            setImageBitmap(BitmapFactory.decodeResource(
                    this.resources, if (color == 0) R.drawable.ch_white else R.drawable.ch_black)) }
    }

    fun place(position: Position) {
        this.position = position
        AndroidBoard.positionToViewCoords(position).apply {
            view.x = first
            view.y = second
        }
    }

    fun place(position: String) = place(Position(position))

    fun addToBoard(board: ViewGroup) {
        board.addView(view, checkerSize, checkerSize)
    }

    fun removeFromBoard(board: ViewGroup) {
        board.removeView(view)
    }

    fun queen() {
        val recourse = if (color == 0) R.drawable.ch_q_white else R.drawable.ch_q_black
        isQueen = true
        view.apply { setImageBitmap(BitmapFactory.decodeResource(resources, recourse)) }
    }
}

class AndroidBoard(private val context: Context, private val board: ViewGroup) {
    val checkers = mutableListOf<AndroidChecker>()
    init {
        val whiteCheckers = listOf("a1", "c1", "e1", "g1", "b2", "d2", "f2", "h2", "a3", "c3", "e3", "g3")
        val blackCheckers = listOf("b8", "d8", "f8", "h8", "a7", "c7", "e7", "g7", "b6", "d6", "f6", "h6")
        init(whiteCheckers, blackCheckers)
    }

    companion object {
        var rotate = false

        fun positionToCoords(position: Position) = (if (!rotate) position.x else 9 - position.x) to (if (!rotate) 9 - position.y else position.y)

        fun positionToViewCoords(position: Position): Pair<Float, Float> {
            val (x, y) = positionToCoords(position)
            val checkerSize = fieldSize *0.8f
            return (x - 0.5f) * fieldSize + checkerSize/2 to (y - 0.5f) * fieldSize + checkerSize/2
        }
    }

    private fun init(whiteCheckers: List<String>, blackCheckers: List<String>) {
        whiteCheckers.map { AndroidChecker(context, 0).apply { place(it) } }.forEach { add(it) }
        blackCheckers.map { AndroidChecker(context, 1).apply { place(it) } }.forEach { add(it) }
    }

    private fun add(checker: AndroidChecker) {
        checker.addToBoard(board)
        checkers.add(checker)
    }

    private fun move(from: Position, to: Position) {
        val checker = checkers.find { it.position == from } ?: return
        checker.place(to)
    }

    fun move(from: String, to: String) = move(Position(from), Position(to))

    fun remove(position: Position) {
        val checker = checkers.find { it.position == position } ?: return
        checkers.remove(checker)
        checker.removeFromBoard(board)
    }

    fun getChecker(position: String) = checkers.find { it.position == Position(position) }
}

class AndroidGame(private val context: Context, private val rootLayout: FrameLayout, private val boardLayout: GridLayout, val tvResult: TextView, val botColor: Int=1) {
    private val board: AndroidBoard
    private lateinit var fields: List<ImageView>
    private val game = Game()
    private var moves: List<String>
    var activeFields: Set<String>
    private var availableMoveFields: Set<String>? = null
    var from: String? = null
    var bot: Player? = null
    var isAnimated = false

    init {
        drawDesk()
        board = AndroidBoard(context, rootLayout)
        moves = game.nextMoves()
        if (botColor == 0) botMove()
        activeFields = extractActiveFields()
        boardLayout.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val pos: String
                pos = if (!AndroidBoard.rotate) {
                    val x = (motionEvent.x / fieldSize).toInt() + 1
                    val y = 8 - (motionEvent.y / fieldSize).toInt()
                    Position(x, y).toString()
                } else {
                    val x = 8 - (motionEvent.x / fieldSize).toInt()
                    val y = (motionEvent.y / fieldSize).toInt() + 1
                    Position(x, y).toString()
                }
                onClicked(pos)
            }
            true
        }
    }

    private fun onClicked(position: String) {
        if (game.currentColor == botColor) return
        Log.d("mych","Touch coordinates :  $position")
        // снять выделение
        clearSelected()
        // пометить доступные для хода фигура
        activeFields.forEach { highlightAvailableCheckers(it) }
        if (position in activeFields) {
            highlightMove(position)
            // пометить доступные ходы для выбранной фигуры
            availableMoveFields = getCheckerMoveFields(position).apply {
                forEach { highlightMove(it) }
            }
            from = position
        } else {
            availableMoveFields?.let {
                if (position in it) {
                    Log.d("mych","Move from $from to $position")
                    nextStep(getCommand(from!!, position))
                    clearSelected()
                    botMove()
                }
            }
            availableMoveFields = null
            from = null
        }
    }

    fun botMove() = async(UI) {
        while (isAnimated) {
            delay(300)
        }
        val command = bg {
            botStep()
        }
        nextStep(command.await())
    }

    fun botStep(): String {
        Log.d("mych", "botStep")
        if (moves.isEmpty()) return ""
        if (bot == null) bot = Player(network.nw, 2)
        return bot!!.selectMove(game.checkerboard, game.currentColor, moves)
    }

    private var stepCounter = 0.0

    private val stepLimit = 50

    private fun nextStep(command: String) {
        if (stepCounter.toInt() == stepLimit) {
            win(-1)
            return
        }
        stepCounter += 0.5
        val currentColor = game.currentColor
        game.go(command)
//        Log.d("mych", "$currentColor: $command")
//        Log.d("mych", game.print())
        val positions = if (command.contains("-")) command.split("-")
        else command.split(":")
        val from = positions.first()
        val to = positions.last()
        showTrack(positions)
        async(UI) {
            while (isAnimated) {
                delay(300)
            }
            board.move(from, to)
            if (command.contains(":")) {
                removeKilledCheckers(currentColor)
            }
            val checker = board.getChecker(to)!!
            if (!checker.isQueen && game.checkerboard.get(to)!!.checker!!.type == 1) {
                Log.d("mych", "${checker.position}")
                checker.queen()
            }
        }
        // перевести ход на соперника
        game.currentColor = 1 - game.currentColor
        moves = game.nextMoves()
        if (moves.isEmpty()) {
            win(1 - game.currentColor)
        } else activeFields = extractActiveFields()
    }

    private fun win(color: Int) {
        tvResult.text =  when (color) {
            0 -> "Белые победили"
            1 -> "Чёрные победили"
            else -> "Ничья"
        }
        boardLayout.setOnTouchListener(null)
    }

    private fun showTrack(positions: List<String>) {
        isAnimated = true
        val checker = board.getChecker(positions.first())!!.view
        checker.bringToFront()
        val path = Path().apply { positions.map { Position(it) }.map { AndroidBoard.positionToViewCoords(it) }
                    .forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) } }
        val objectAnimator = ObjectAnimator.ofFloat(checker, View.X, View.Y, path)
        objectAnimator.duration = 500L * (positions.size-1)
        val animationSet = AnimatorSet()
        animationSet.play(objectAnimator)
        animationSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                isAnimated = false
            }
        })
        animationSet.start()
    }

    /** Отобразить состояние доски **/
    private fun removeKilledCheckers(currentColor: Int) {
        board.checkers.filter { it.color != currentColor }.map { it.position }.filter {
            game.checkerboard.get(it)!!.checker == null
        }.forEach {
            Log.d("mych", "removeKilledCheckers $currentColor $it")
            board.remove(it)
        }
    }

    /** Очистить выделение полей на доске **/
    private fun clearSelected() = game.checkerboard.board.filter { it.color == 1 }
            .map { Position(it.x, it.y) }.forEach { highlightField(it.toString(), R.drawable.dark_oak) }

    private fun highlightField(position: String, recourse: Int) = Position(position).apply {
        val i = (8 - y) * 4 + x / 2 + x % 2 - 1
        val index = if (!AndroidBoard.rotate) i else 31 - i
        fields[index].apply { setImageBitmap(BitmapFactory.decodeResource(resources, recourse)) }
    }

    fun highlightMove(position: String) = highlightField(position, R.drawable.dark_oak_active_darker)

    fun highlightAvailableCheckers(position: String) = highlightField(position, R.drawable.dark_oak_active_lighter)

    private fun drawDesk() {
        boardLayout.columnCount = 8
        boardLayout.rowCount = 8
        val whiteFieldIndexes = listOf(1, 3, 5, 7, 10, 12, 14, 16, 17, 19, 21, 23, 26, 28, 30, 32, 33, 35, 37, 39, 42, 44, 46, 48, 49, 51, 53, 55, 58, 60, 62, 64)
        val fields = (1..64).map { if (it in whiteFieldIndexes) R.drawable.white_oak else R.drawable.dark_oak }
                .map { ImageView(context).apply { setImageBitmap(BitmapFactory.decodeResource(this.resources, it)) } }
        this.fields = fields.filterIndexed { i, _ -> i + 1 !in whiteFieldIndexes }
        fields.forEach { boardLayout.addView(it, fieldSize, fieldSize) }
        var letters = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        if (AndroidBoard.rotate) letters = letters.asReversed()
        for (row in listOf(20f, fieldSize * 9f)) {
            letters.forEachIndexed { i, s ->
                rootLayout.addView(TextView(context).apply {
                    text = Html.fromHtml("<b>$s</b>", Html.FROM_HTML_MODE_LEGACY)
                    x = fieldSize.toFloat() * (i + 1) + 15
                    y = row
                    z = 1000f
                })
            }
        }
        for (col in listOf(30f, fieldSize * 9f + 10f)) {
            (1..8).forEach {
                rootLayout.addView(TextView(context).apply {
                    val number = if (!AndroidBoard.rotate) 9-it else it
                    text = Html.fromHtml("<b>$number</b>", Html.FROM_HTML_MODE_LEGACY)
                    x = col
                    y = fieldSize.toFloat() * it
                    z = 1000f
                })
            }
        }
    }

    /** Возвращает список позиций шашек, которые могут ходить **/
    private fun extractActiveFields() = moves.map { it.substring(0, 2) }.toSet()

    /** Возвращает список позиций полей на которых оканчиваются ходы шашки с позицией [position] **/
    private fun getCheckerMoveFields(position: String) = moves
            .filter { it.substring(0, 2) == position }
            .map { it.substring(it.length-2, it.length) }.toSet()

    /** Возвращает команду для шашки с позиции [from], которая перемещается на позицию [to] **/
    private fun getCommand(from: String, to: String) = moves
            .filter { it.substring(0, 2) == from }.first { it.substring(it.length - 2, it.length) == to }
}

fun <T> Stream<T>.toList() = this.collect(Collectors.toList())

class MainActivity : AppCompatActivity() {
    private val sceneRoot get() = findViewById<FrameLayout>(R.id.sceneRoot)
    private val board get() = findViewById<GridLayout>(R.id.board)
    private val tvResult get() = findViewById<TextView>(R.id.tvResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        val res = resources.assets.open("best.net").bufferedReader()
        val list = res.lines().toList()
        network.nw = NetworkIO().load(list)!!
        res.close()
        AndroidGame(layoutInflater.context, sceneRoot, board, tvResult)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        return if (id == R.id.action_play_white || id == R.id.action_play_black) {
            AndroidBoard.rotate = id == R.id.action_play_black
            board.removeAllViews()
            (0 until sceneRoot.childCount).map { sceneRoot.getChildAt(it) }.filter { it.id != R.id.desk }.forEach {
                sceneRoot.removeView(it)
            }
            tvResult.text = ""
            AndroidGame(layoutInflater.context, sceneRoot, board, tvResult, 0.takeIf { AndroidBoard.rotate } ?: 1)
            true
        } else super.onOptionsItemSelected(item)
    }
}
