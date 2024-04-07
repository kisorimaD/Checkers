import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.datetime.*
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.Window
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.lang.Exception
import java.lang.String.format
import javax.swing.WindowConstants
import kotlin.math.*
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

var turn = false
var chosenCell = Pair(-1, -1)
var clickedCell = Pair(-1, -1)
var fixedCell = Pair(-1, -1)

var beginCell = Pair(-1, -1)
var possibleMoves = mutableListOf<Pair<Int, Int>>()
var possibleEatMoves = mutableListOf< Pair <Pair <Int, Int> , Pair < Int, Int> > >()

var gameOver = false
var winTurn = "?"

val whitePieces: MutableList<Pair<Int, Int>> = mutableListOf()
val blackPieces: MutableList<Pair<Int, Int>> = mutableListOf()

val whiteQueens : MutableList<Pair<Int, Int>> = mutableListOf()
val blackQueens : MutableList<Pair<Int, Int>> = mutableListOf()

val indent = 20f
val imagePath = "images/board.jpg"
var image = Image.makeFromEncoded(File(imagePath).readBytes())


fun main(args : Array<String>) {
    var turnString = if(args.isNotEmpty()) args[0] else "white"


    turn = (turnString == "white")
    createWindow("Checkers")

}
fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseMotionListener(MouseMotionAdapter)
    window.layer.addKeyListener(KeyListener)
    window.layer.addMouseListener(MouseListener)

    window.preferredSize = Dimension(800, 600)
    window.minimumSize = Dimension(400, 400)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true


}
class Renderer(val layer: SkiaLayer) : SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/ArialBold.ttf")
    val font = Font(typeface, 40f)
    val paint = Paint().apply {
        color = 0xff9BC730L.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }

    val blackCell = Paint().apply {
        color = 0xFFA0522D.toInt()
        mode = PaintMode.FILL
    }

    val whiteCell = Paint().apply {
        color = 0xFFFFEBCD.toInt()
        mode = PaintMode.FILL
    }

    val blackPiece = Paint().apply {
        color = 0xFF000000.toInt()
        mode = PaintMode.FILL
    }

    val whitePiece = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        mode = PaintMode.FILL
    }

    val greyNumbers = Paint().apply {
        color = 0xFFC4C4C4.toInt()
        mode = PaintMode.FILL
    }

    val clickedColor = Paint().apply {
        color = 0xFFFF0000.toInt()
        mode = PaintMode.FILL
    }

    val chosenColor = Paint().apply {
        color = 0xFF00FF00.toInt()
        mode = PaintMode.FILL
    }

    val possibleMoveColor = Paint().apply {
        color = 0xFF2D7BA0.toInt()
        mode = PaintMode.FILL
    }




    fun placePieces() {

        for (x in 0..7) {
            whitePieces.add(Pair(x, if (x % 2 == 0) 1 else 0))
        }

        for (x in 1..7 step 2) {
            whitePieces.add(Pair(x, 2))
        }

        for (x in 0..7) {
            blackPieces.add(Pair(x, if (x % 2 == 0) 7 else 6))
        }

        for (x in 0..7 step 2) {
            blackPieces.add(Pair(x, 5))
        }
        //whitePieces.add(Pair(0, 1))
        //blackPieces.add(Pair(0,7))

    }

    init {
        placePieces()
    }

    @ExperimentalTime
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        //println(clickedCell)
        if(gameOver)
        {
            fillScreen(canvas, width, height)
            displayPieces(canvas, width, height)

            val w = (width / layer.contentScale).toInt()
            val h = (height / layer.contentScale).toInt()
            val cellSize = (min(w, h) - 2 * indent) / 9f

            val centerX = w / 2f
            val centerY = h / 2f

            val firstX = centerX - 4 * cellSize - indent
            val firstY = centerY - 4 * cellSize - indent

            val boardIndent = cellSize / 3

            canvas.drawRect(
                Rect.makeXYWH(
                    firstX - boardIndent + indent,
                    firstY - boardIndent + indent + 3 * cellSize,
                    8 * cellSize + 2 * boardIndent,
                    2 * cellSize + 2 * boardIndent
                ), blackPiece
            )


            canvas.drawString(
                ("${winTurn.uppercase()} WON").toString(),
                firstX + indent + 4 * cellSize - ("${winTurn.uppercase()} WON".length * (cellSize * 32 / 50) / 2),
                firstY - boardIndent + indent + 4 * cellSize + cellSize * 30 / 50,
                Font(typeface, cellSize),
                greyNumbers
            )

            //println(winTurn)
            return
        }

        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()

        findCell(State.mouseX, State.mouseY, width, height)

        fillScreen(canvas, width, height)

        displayPieces(canvas, width, height)

        //displayTime(canvas, now.toLocalDateTime(timeZone))

        layer.needRedraw()
    }

    // fill all screen with image
    fun fillScreen(canvas: Canvas, width: Int, height: Int) {
        val w = (width / layer.contentScale).toInt()
        val h = (height / layer.contentScale).toInt()
        canvas.drawImageRect(image, Rect.makeXYWH(0f, 0f, w.toFloat(), h.toFloat()))
    }



    private fun findCell(x: Float, y: Float, width: Int, height: Int){
        val w = (width / layer.contentScale).toInt()
        val h = (height / layer.contentScale).toInt()
        val cellSize = (min(w, h) - 2 * indent) / 9f

        val centerX = w / 2f
        val centerY = h / 2f

        val firstX = centerX - 4 * cellSize - indent
        val firstY = centerY - 4 * cellSize - indent

        if(x < firstX + indent || x > firstX + 8 * cellSize + indent || y < firstY + indent || y > firstY + 8 * cellSize + indent)
        {
            chosenCell = Pair(-1, -1)
            return
        }

        val xCell = ((x - firstX - indent) / cellSize).toInt()
        val yCell = ((y - firstY - indent) / cellSize).toInt()

        chosenCell = Pair(xCell, yCell)
    }



    fun needPaint(x: Int, y: Int): Paint {
        return when
        {
            Pair(x,y) == clickedCell -> clickedColor
            Pair(x,y) == chosenCell -> chosenColor
            Pair(x,y) in possibleMoves -> possibleMoveColor
            (x + y) % 2 == 0 -> whiteCell
            else -> blackCell
        }
    }

    private fun displayPieces(canvas: Canvas, width: Int, height: Int) {
        val w = (width / layer.contentScale).toInt()
        val h = (height / layer.contentScale).toInt()
        val cellSize = (min(w, h) - 2 * indent) / 9f

        val centerX = w / 2f
        val centerY = h / 2f

        val firstX = centerX - 4 * cellSize - indent
        val firstY = centerY - 4 * cellSize - indent

        // black rectangle for board
        val boardIndent = cellSize / 3

        canvas.drawCircle(
            firstX - boardIndent + indent + 8 * cellSize + boardIndent,
            firstY - boardIndent + indent + (8 * cellSize + 2 * boardIndent) / 2,
            cellSize * 2 / 3,
            if(turn) whitePiece else blackPiece
        )

        canvas.drawRect(
            Rect.makeXYWH(
                firstX - boardIndent + indent,
                firstY - boardIndent + indent,
                8 * cellSize + 2 * boardIndent,
                8 * cellSize + 2 * boardIndent
            ), blackPiece
        )

        for (x in 0..7) {
            for (y in 0..7) {
                canvas.drawRect(
                    Rect.makeXYWH(
                        firstX + x * cellSize + indent,
                        firstY + y * cellSize + indent,
                        cellSize,
                        cellSize),
                    needPaint(x, y))
            }
        }

        val fontSize = cellSize / 3
        for (x in 0..7) {
            canvas.drawString(
                ('a' + x).toString(),
                firstX + x * cellSize + indent + cellSize / 2 - (fontSize * 3 / 10),
                firstY - boardIndent / 2 + indent + (fontSize * 3 / 10),
                Font(typeface, fontSize),
                greyNumbers
            )
            canvas.drawString(
                ('a' + x).toString(),
                firstX + x * cellSize + indent + cellSize / 2 - (fontSize * 3 / 10),
                firstY + boardIndent / 2 + 8 * cellSize + indent + (fontSize * 3 / 10),
                Font(typeface, fontSize),
                greyNumbers
            )
            canvas.drawString(
                (8 - x).toString(),
                firstX + indent - boardIndent / 2 - (fontSize * 3 / 10),
                firstY + x * cellSize + indent + cellSize * 2 / 3,
                Font(typeface, fontSize),
                greyNumbers
            )
            canvas.drawString(
                (8 - x).toString(),
                firstX + 8 * cellSize + indent + boardIndent / 2 - (fontSize * 3 / 10),
                firstY + x * cellSize + indent + cellSize * 2 / 3,
                Font(typeface, fontSize),
                greyNumbers
            )
        }

        for (x in whitePieces) {
            canvas.drawCircle(
                firstX + x.first * cellSize + indent + cellSize / 2,
                firstY + x.second * cellSize + indent + cellSize / 2,
                cellSize * 2 / 5,
                whitePiece
            )

            if(x in whiteQueens)
                canvas.drawCircle(
                    firstX + x.first * cellSize + indent + cellSize / 2,
                    firstY + x.second * cellSize + indent + cellSize / 2,
                    cellSize / 5,
                    blackPiece
                )
        }
        for (x in blackPieces) {
            canvas.drawCircle(
                firstX + x.first * cellSize + indent + cellSize / 2,
                firstY + x.second * cellSize + indent + cellSize / 2,
                cellSize * 2 / 5,
                blackPiece
            )

            if(x in blackQueens)
                canvas.drawCircle(
                    firstX + x.first * cellSize + indent + cellSize / 2,
                    firstY + x.second * cellSize + indent + cellSize / 2,
                    cellSize / 5,
                    whitePiece
                )
        }

    }

    private fun displayTime(canvas: Canvas, localDateTime: LocalDateTime) {
        val text = format("%02d:%02d:%02d", localDateTime.hour, localDateTime.minute, localDateTime.second)

        canvas.drawString(text, State.mouseX, State.mouseY, font, paint)
    }

}

fun checkEatMoves(turn : Boolean)
{
    possibleEatMoves.clear()
    var isAvailableMoves = false

    if(turn)
    {
        for(x in whitePieces) {
            possibleMovesChecker(x.first, x.second)
            if(possibleMoves.isNotEmpty())
                isAvailableMoves = true
        }
        possibleMoves.clear()
    }
    else
    {
        for(x in blackPieces) {
            possibleMovesChecker(x.first, x.second)
            if(possibleMoves.isNotEmpty())
                isAvailableMoves = true
        }
        possibleMoves.clear()
    }

    if(!isAvailableMoves)
    {
        gameOver = true
        winTurn = if(turn) "black" else "white"
    }
}

fun possibleMovesChecker(x : Int, y : Int) {
    possibleMoves.clear()
    beginCell = Pair(x, y)

    if(turn)
    {
        if(Pair(x, y) !in whitePieces)
            return

        if(Pair(x,y) !in whiteQueens)
        {
            if(Pair(x + 1, y + 1) !in whitePieces && Pair(x + 1, y + 1) !in blackPieces)
                possibleMoves.add(Pair(x + 1, y + 1))

            if(Pair(x + 1, y + 1) in blackPieces && Pair(x + 2, y + 2) !in whitePieces && Pair(x + 2, y + 2) !in blackPieces) {
                possibleMoves.add(Pair(x + 2, y + 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x + 2, y + 2)))
            }
            if(Pair(x - 1, y + 1) !in whitePieces && Pair(x - 1, y + 1) !in blackPieces)
                possibleMoves.add(Pair(x - 1, y + 1))

            if(Pair(x - 1, y + 1) in blackPieces && Pair(x - 2, y + 2) !in whitePieces && Pair(x - 2, y + 2) !in blackPieces) {
                possibleMoves.add(Pair(x - 2, y + 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x - 2, y + 2)))
            }
            // взятие назад

            if(Pair(x - 1, y - 1) in blackPieces && Pair(x - 2, y - 2) !in whitePieces && Pair(x - 2, y - 2) !in blackPieces) {
                possibleMoves.add(Pair(x - 2, y - 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x - 2, y - 2)))
            }
            if(Pair(x + 1, y - 1) in blackPieces && Pair(x + 2, y - 2) !in whitePieces && Pair(x + 2, y - 2) !in blackPieces) {
                possibleMoves.add(Pair(x + 2, y - 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x + 2, y - 2)))
            }
        }
        else
        {
            var wasEnemy = false
            // LEFT DOWN

            wasEnemy = false
            for(i in 1 .. min(x, 7 - y))
            {
                if(Pair(x - i, y + i) !in whitePieces && Pair(x - i, y + i) !in blackPieces) {
                    possibleMoves.add(Pair(x - i, y + i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x - i, y + i)))
                }
                if(Pair(x - i, y + i) in whitePieces)
                    break

                if(Pair(x - i, y + i) in blackPieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x - i - 1, y + i + 1) !in whitePieces && Pair(x - i - 1, y + i + 1) !in blackPieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }



            // RIGHT DOWN
            wasEnemy = false
            for(i in  1..min(7 - x, 7 - y))
            {
                if(Pair(x + i, y + i) !in whitePieces && Pair(x + i, y + i) !in blackPieces) {
                    possibleMoves.add(Pair(x + i, y + i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x + i, y + i)))
                }

                if(Pair(x + i, y + i) in whitePieces)
                    break

                if(Pair(x + i, y + i) in blackPieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x + i + 1, y + i + 1) !in whitePieces && Pair(x + i + 1, y + i + 1) !in blackPieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }

            // LEFT UP
            wasEnemy = false
            for(i in 1 .. min(x, y))
            {
                if(Pair(x - i, y - i) !in whitePieces && Pair(x - i, y - i) !in blackPieces) {
                    possibleMoves.add(Pair(x - i, y - i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x - i, y - i)))
                }

                if(Pair(x - i, y - i) in whitePieces)
                    break

                if(Pair(x - i, y - i) in blackPieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x - i - 1, y - i - 1) !in whitePieces && Pair(x - i - 1, y - i - 1) !in blackPieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }

            // RIGHT UP
            wasEnemy = false
            for(i in 1 .. min(7 - x, y))
            {
                if(Pair(x + i, y - i) !in whitePieces && Pair(x + i, y - i) !in blackPieces) {
                    possibleMoves.add(Pair(x + i, y - i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x + i, y - i)))
                }

                if(Pair(x + i, y - i) in whitePieces)
                    break

                if(Pair(x + i, y - i) in blackPieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x + i + 1, y - i - 1) !in whitePieces && Pair(x + i + 1, y - i - 1) !in blackPieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }
        }
    }
    else
    {
        if(Pair(x, y) !in blackPieces)
            return

        if(Pair(x,y) !in blackQueens) 
        {
            if (Pair(x + 1, y - 1) !in whitePieces && Pair(x + 1, y - 1) !in blackPieces)
                possibleMoves.add(Pair(x + 1, y - 1))

            if (Pair(x + 1, y - 1) in whitePieces && Pair(x + 2, y - 2) !in whitePieces && Pair(x + 2, y - 2) !in blackPieces) {
                possibleMoves.add(Pair(x + 2, y - 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x + 2, y - 2)))
            }
            if (Pair(x - 1, y - 1) !in whitePieces && Pair(x - 1, y - 1) !in blackPieces)
                possibleMoves.add(Pair(x - 1, y - 1))

            if (Pair(x - 1, y - 1) in whitePieces && Pair(x - 2, y - 2) !in whitePieces && Pair(x - 2, y - 2) !in blackPieces) {
                possibleMoves.add(Pair(x - 2, y - 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x - 2, y - 2)))
            }
            // взятие назад

            if(Pair(x - 1, y + 1) in whitePieces && Pair(x - 2, y + 2) !in whitePieces && Pair(x - 2, y + 2) !in blackPieces) {
                possibleMoves.add(Pair(x - 2, y + 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x - 2, y + 2)))
            }

            if(Pair(x + 1, y + 1) in whitePieces && Pair(x + 2, y + 2) !in whitePieces && Pair(x + 2, y + 2) !in blackPieces) {
                possibleMoves.add(Pair(x + 2, y + 2))
                possibleEatMoves.add(Pair(beginCell, Pair(x + 2, y + 2)))
            }
        }
        else
        {
            var wasEnemy = false
            // LEFT DOWN

            wasEnemy = false
            for(i in 1 .. min(x, 7 - y))
            {
                if(Pair(x - i, y + i) !in blackPieces && Pair(x - i, y + i) !in whitePieces) {
                    possibleMoves.add(Pair(x - i, y + i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x - i, y + i)))
                }
                if(Pair(x - i, y + i) in blackPieces)
                    break

                if(Pair(x - i, y + i) in whitePieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x - i - 1, y + i + 1) !in blackPieces && Pair(x - i - 1, y + i + 1) !in whitePieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }



            // RIGHT DOWN
            wasEnemy = false
            for(i in  1..min(7 - x, 7 - y))
            {
                if(Pair(x + i, y + i) !in blackPieces && Pair(x + i, y + i) !in whitePieces) {
                    possibleMoves.add(Pair(x + i, y + i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x + i, y + i)))
                }
                if(Pair(x + i, y + i) in blackPieces)
                    break

                if(Pair(x + i, y + i) in whitePieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x + i + 1, y + i + 1) !in blackPieces && Pair(x + i + 1, y + i + 1) !in whitePieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }

            // LEFT UP
            wasEnemy = false
            for(i in 1 .. min(x, y))
            {
                if(Pair(x - i, y - i) !in blackPieces && Pair(x - i, y - i) !in whitePieces) {
                    possibleMoves.add(Pair(x - i, y - i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x - i, y - i)))
                }
                if(Pair(x - i, y - i) in blackPieces)
                    break

                if(Pair(x - i, y - i) in whitePieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x - i - 1, y - i - 1) !in blackPieces && Pair(x - i - 1, y - i - 1) !in whitePieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }

            // RIGHT UP
            wasEnemy = false
            for(i in 1 .. min(7 - x, y))
            {
                if(Pair(x + i, y - i) !in blackPieces && Pair(x + i, y - i) !in whitePieces) {
                    possibleMoves.add(Pair(x + i, y - i))
                    if(wasEnemy)
                        possibleEatMoves.add(Pair(beginCell, Pair(x + i, y - i)))
                }

                if(Pair(x + i, y - i) in blackPieces)
                    break

                if(Pair(x + i, y - i) in whitePieces)
                {
                    if(wasEnemy)
                        break

                    if(Pair(x + i + 1, y - i - 1) !in blackPieces && Pair(x + i + 1, y - i - 1) !in whitePieces)
                    {
                        wasEnemy = true
                        continue
                    }
                    break
                }
            }
        }
    }
}

fun movePiece(x: Int, y : Int) : Boolean
{
    if(Pair(x,y) in possibleMoves)
    {
        val isQueen = if(turn) (beginCell in whiteQueens) else (beginCell in blackQueens)

        if(turn)
        {
            whitePieces.remove(beginCell)
            if(isQueen)
                whiteQueens.remove(beginCell)

            whitePieces.add(Pair(x, y))
            if(isQueen)
                whiteQueens.add(Pair(x, y))

            if(!isQueen && y == 7)
                whiteQueens.add(Pair(x, y))


            val dirX = if(x > beginCell.first) 1 else -1
            val dirY = if(y > beginCell.second) 1 else -1

            var nowCell = beginCell
            for(i in 1 until abs(x - beginCell.first))
            {
                nowCell = Pair(nowCell.first + dirX, nowCell.second + dirY)
                if(nowCell in blackPieces)
                {
                    blackPieces.remove(nowCell)
                    if(nowCell in blackQueens)
                        blackQueens.remove(nowCell)
                    break
                }
            }

            beginCell = Pair(-1,-1) /// TODO
            possibleMoves.clear()
            possibleEatMoves.clear()
        }
        else
        {
            blackPieces.remove(beginCell)
            if (isQueen)
                blackQueens.remove(beginCell)

            blackPieces.add(Pair(x, y))
            if (isQueen)
                blackQueens.add(Pair(x, y))

            if(!isQueen && y == 0)
                blackQueens.add(Pair(x, y))


            val dirX = if(x > beginCell.first) 1 else -1
            val dirY = if(y > beginCell.second) 1 else -1

            var nowCell = beginCell
            for(i in 1 until abs(x - beginCell.first))
            {
                nowCell = Pair(nowCell.first + dirX, nowCell.second + dirY)
                if(nowCell in whitePieces)
                {
                    whitePieces.remove(nowCell)
                    if(nowCell in whiteQueens)
                        whiteQueens.remove(nowCell)
                    break
                }
            }

            beginCell = Pair(-1,-1) /// TODO
            possibleMoves.clear()
            possibleEatMoves.clear()
        }
        return true
    }
    else
        return false
}


object State {
    var mouseX = 0f
    var mouseY = 0f
}

object MouseMotionAdapter : MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent) {
        State.mouseX = event.x.toFloat()
        State.mouseY = event.y.toFloat()
    }


}

object KeyListener : KeyListener {
    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent?) {

    }

    override fun keyReleased(e: KeyEvent?) {
        /*if(e?.keyChar == 'f') {
            turn = !turn
            clickedCell = Pair(-1, -1)
            checkEatMoves(turn)
        }*/
    }
}

fun clearEatMoves()
{
    possibleEatMoves = possibleEatMoves.filter { move ->
        val beg = move.first
        val dest = move.second

        ((beg.first < 8) && (beg.first >= 0)) && ((beg.second < 8) && (beg.second >= 0)) && ((dest.first < 8) && (dest.first >= 0) && ((dest.second < 8) && (dest.second >= 0)))
    }.toMutableList()
}

object MouseListener : MouseListener {
    override fun mouseClicked(e: MouseEvent?) {

    }

    override fun mousePressed(e: MouseEvent?) {
        if(e?.button == MouseEvent.BUTTON1) {
            //println("MousePressed")

            if(fixedCell != Pair(-1, -1) && (Pair(fixedCell, chosenCell) !in possibleEatMoves))
                return

            clickedCell = chosenCell

            val checkersNum = blackPieces.size + whitePieces.size

            if (movePiece(chosenCell.first, chosenCell.second)) {
                possibleMovesChecker(chosenCell.first, chosenCell.second) // new beginCell
                clearEatMoves()
                if(checkersNum == blackPieces.size + whitePieces.size || possibleEatMoves.isEmpty())
                {
                    fixedCell = Pair(-1, -1)
                    turn = !turn
                    checkEatMoves(turn)
                }
                else
                {
                    fixedCell = chosenCell

                    possibleMoves = possibleMoves.filter { Pair(Pair(chosenCell.first, chosenCell.second), it) in possibleEatMoves }.toMutableList()


                    //possibleMovesChecker(fixedCell.first, fixedCell.second)
                }
            }
            else
            {
                //println(possibleEatMoves)
                //clickedCell = Pair(-1, -1)
                possibleMovesChecker(chosenCell.first, chosenCell.second) // new beginCell
                clearEatMoves()
                if(possibleEatMoves.isNotEmpty())
                {
                    possibleMoves = possibleMoves.filter { Pair(Pair(chosenCell.first, chosenCell.second), it) in possibleEatMoves }.toMutableList()
                }
            }
        }
    }

    override fun mouseReleased(e: MouseEvent?) {
    }

    override fun mouseEntered(e: MouseEvent?) {
    }

    override fun mouseExited(e: MouseEvent?) {
    }
}

//fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float) = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)