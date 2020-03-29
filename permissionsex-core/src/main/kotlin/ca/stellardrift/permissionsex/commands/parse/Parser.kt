package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.glob.parser.CommandLexer
import ca.stellardrift.permissionsex.util.glob.parser.CommandParser
import ca.stellardrift.permissionsex.util.glob.parser.CommandParserBaseListener
import ca.stellardrift.permissionsex.util.globs.GlobMessages
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker


fun parseFrom(text: String): List<CommandToken> {
    return parseFrom(CharStreams.fromString(text))
}

fun parseFrom(stream: CharStream): List<CommandToken> {
    val lexer = CommandLexer(stream)
    val tokenStream = CommonTokenStream(lexer)
    val parser = CommandParser(tokenStream)
    parser.errorHandler = BailErrorStrategy()
    val walker = ParseTreeWalker()
    val listener = CommandListener()

    try {
        walker.walk(listener, parser.command())
    } catch (e: ParseCancellationException) {
        val ex = e.cause as RecognitionException?
        val errorToken = ex!!.offendingToken
        throw CommandException(
            GlobMessages.ERROR_PARSE(errorToken.text, errorToken.line, errorToken.charPositionInLine), ex
        )
    }

    return listener.nodes
}

class CommandArgs(val args: List<CommandToken>) {
    private var idx = 0
    
    var state: Any get() = idx
        set(state) {idx = state as Int}
}

sealed class CommandToken(val contents: String, val startPosition: Int, val endPosition: Int)

class ShortFlag(contents: Char, startPosition: Int, endPosition: Int): CommandToken(contents.toString(), startPosition, endPosition)
class LongFlag(contents: String, startPosition: Int, endPosition: Int, val fixedValue: String?): CommandToken(contents, startPosition, endPosition)
class Word(contents: String, startPosition: Int, endPosition: Int): CommandToken(contents, startPosition, endPosition)

internal class CommandListener: CommandParserBaseListener() {
    val nodes: MutableList<CommandToken> = mutableListOf()

}
