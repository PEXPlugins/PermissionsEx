package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommonMessages
import ca.stellardrift.permissionsex.util.command.args.ArgumentParseException
import ca.stellardrift.permissionsex.util.glob.parser.CommandLexer
import ca.stellardrift.permissionsex.util.glob.parser.CommandParser
import ca.stellardrift.permissionsex.util.glob.parser.CommandParserBaseListener
import ca.stellardrift.permissionsex.util.globs.GlobMessages
import com.google.common.collect.Lists
import com.google.common.collect.PeekingIterator
import net.kyori.text.Component
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.util.*
import java.util.function.Predicate


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

class CommandArgs(args: List<CommandToken>) {
    var args: MutableList<CommandToken> private set

    init {
        this.args = args.toMutableList()
    }

    private var idx = 0

    var state: Any get() = idx
        set(state) {idx = state as Int}

    /**
     * Return the position of the last next() call, or -1 if next() has never been called
     *
     * @return The current position
     */
    var position = -1

    operator fun hasNext(): Boolean {
        return position + 1 < args.size
    }

    @Throws(ArgumentParseException::class)
    fun peek(): String {
        if (!hasNext()) {
            throw createError(CommonMessages.ERROR_ARGUMENTS_NOTENOUGH())
        }
        return args[position + 1].contents
    }

    @Throws(ArgumentParseException::class)
    operator fun next(): String {
        if (!hasNext()) {
            throw createError(CommonMessages.ERROR_ARGUMENTS_NOTENOUGH())
        }
        return args[++position].contents
    }

    fun nextIfPresent(): String? {
        return if (hasNext()) args[++position].contents else null
    }

    fun createError(message: Component): ArgumentParseException {
        //System.out.println("Creating error: " + message.translateFormatted(Locale.getDefault()));
        //Thread.dumpStack();
        return ArgumentParseException(message, "", if (position < 0) 0 else args[position].startPosition)
    }

    val all: List<String>
        get() = args.map { it.contents }

    fun filterArgs(filter: (String) -> Boolean) {
        val currentArg = if (position == -1) null else args[position]
        val newArgs: MutableList<CommandToken> = args.filter {
            filter(it.contents)
        }.toMutableList()
        position = if (currentArg == null) -1 else newArgs.indexOf(currentArg)
        args = newArgs
    }

    fun insertArg(value: String) {
        val index = if (position < 0) 0 else args[position].endPosition
        args.add(index, Word(value, index, index))
    }

    fun removeArgs(startIdx: Int, endIdx: Int) {
        if (position >= startIdx) {
            if (position < endIdx) {
                position = startIdx - 1
            } else {
                position -= endIdx - startIdx + 1
            }
        }
        for (i in startIdx..endIdx) {
            args.removeAt(startIdx)
        }
    }
}

sealed class CommandToken(val contents: String, val startPosition: Int, val endPosition: Int)

class ShortFlag(contents: Char, startPosition: Int, endPosition: Int): CommandToken(contents.toString(), startPosition, endPosition)
class LongFlag(contents: String, startPosition: Int, endPosition: Int, val fixedValue: String?): CommandToken(contents, startPosition, endPosition)
class Word(contents: String, startPosition: Int, endPosition: Int): CommandToken(contents, startPosition, endPosition)

internal class CommandListener: CommandParserBaseListener() {
    val nodes: MutableList<CommandToken> = mutableListOf()

}
