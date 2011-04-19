/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.commands;

import com.nijikokun.bukkit.Permissions.Permissions;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author code
 */
public class CommandsManager {

    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected Map<String, Map<CommandSyntax, CommandBinding>> listeners = new HashMap<String, Map<CommandSyntax, CommandBinding>>();
    protected Plugin plugin;

    public CommandsManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandListener listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) {
                continue;
            }

            Command cmdAnotation = method.getAnnotation(Command.class);

            Map<CommandSyntax, CommandBinding> commandListeners = listeners.get(cmdAnotation.name());
            if (commandListeners == null) {
                commandListeners = new HashMap<CommandSyntax, CommandBinding>();
                listeners.put(cmdAnotation.name(), commandListeners);
            }

            commandListeners.put(new CommandSyntax(cmdAnotation.syntax()), new CommandBinding(listener, method));
        }
    }

    public boolean execute(CommandSender sender, org.bukkit.command.Command command, String[] args) {
        Map<CommandSyntax, CommandBinding> callMap = this.listeners.get(command.getName());

        if (callMap == null) { // No commands registred
            return false;
        }

        CommandBinding selectedBinding = null;
        int argumentsLength = 0;
        String arguments = implodeArgs(args);

        for (Entry<CommandSyntax, CommandBinding> entry : callMap.entrySet()) {
            CommandSyntax syntax = entry.getKey();

            if (!syntax.isMatch(arguments)) {
                continue;
            }

            if (selectedBinding != null && syntax.getRegexp().length() < argumentsLength) { // match, but there already more fitted variant
                continue;
            }

            CommandBinding binding = entry.getValue();
            binding.setParams(syntax.getMatchedArguments(arguments));
            selectedBinding = binding;
        }

        if (selectedBinding == null) { // there is fitting handler
            return false;
        }

        // Check permission
        Command commandAnnotation = selectedBinding.getMethodAnnotation();
        if (!commandAnnotation.permission().isEmpty() && sender instanceof Player) { // this method are not public and reqire permission
            if (!Permissions.Security.has((Player) sender, commandAnnotation.permission())) {
                logger.warning("User " + ((Player) sender).getName() + " was tried to access chat command \"" + command.getName() + " " + arguments + "\","
                        + " but have no rights (" + commandAnnotation.permission() + ") to do that.");
                sender.sendMessage(ChatColor.RED + "Sorry, you don't have enough permissions.");
                return true;
            }
        }


        try {
            selectedBinding.call(this.plugin, sender, selectedBinding.getParams());
        } catch (RuntimeException e) {
            logger.severe("There is bogus command handler for " + command.getName() + " command. (Is appopriate plugin is update?)");
        }

        return true;
    }

    protected String implodeArgs(String[] args) {
        String arguments = "";
        for (int i = 0; i < args.length; i++) {
            arguments += " " + args[i];
        }
        return arguments.trim();
    }

    protected class CommandSyntax {

        protected String originalSyntax;
        protected String regexp;
        protected List<String> arguments = new LinkedList<String>();

        public CommandSyntax(String syntax) {
            this.originalSyntax = syntax;

            this.regexp = this.prepareSyntaxRegexp(syntax);
        }

        public String getRegexp() {
            return regexp;
        }

        private String prepareSyntaxRegexp(String syntax) {
            String expression = syntax;

            Matcher argMatcher = Pattern.compile("(?:[\\s]+)((\\<|\\[)([^\\>\\]]+)(?:\\>|\\]))").matcher(expression);
            //Matcher argMatcher = Pattern.compile("(\\<|\\[)([^\\>\\]]+)(?:\\>|\\])").matcher(expression);

            int index = 0;
            while (argMatcher.find()) {
                // Yeah it have been done not most optimal way. Futher refactorings
                if (argMatcher.group(2).equals("[")) {
                    expression = expression.replace(argMatcher.group(0), "(?:(?:[\\s]+)(\"[^\"]+\"|[^\\s]+))?");
                } else {
                    expression = expression.replace(argMatcher.group(1), "(\"[^\"]+\"|[\\S]+)");
                }

                arguments.add(index++, argMatcher.group(3));
            }

            return expression;
        }

        public boolean isMatch(String str) {
            return str.matches(this.regexp);
        }

        public Map<String, String> getMatchedArguments(String str) {
            Map<String, String> matchedArguments = new HashMap<String, String>(this.arguments.size());

            if (this.arguments.size() > 0) {
                Matcher argMatcher = Pattern.compile(this.regexp).matcher(str);

                if (argMatcher.find()) {
                    for (int index = 1; index <= argMatcher.groupCount(); index++) {
                        String argumentValue = argMatcher.group(index);
                        if (argumentValue == null || argumentValue.isEmpty()) {
                            continue;
                        }

                        if (argumentValue.startsWith("\"") && argumentValue.endsWith("\"")) { // Trim boundary colons
                            argumentValue = argumentValue.substring(1, argumentValue.length() - 1);
                        }

                        matchedArguments.put(this.arguments.get(index - 1), argumentValue.trim());
                    }
                }
            }
            return matchedArguments;
        }
    }

    protected class CommandBinding {

        protected Object object;
        protected Method method;
        protected Map<String, String> params = new HashMap<String, String>();

        public CommandBinding(Object object, Method method) {
            this.object = object;
            this.method = method;
        }

        public Command getMethodAnnotation() {
            return this.method.getAnnotation(Command.class);
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public void call(Object... args) {
            try {
                this.method.invoke(object, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
