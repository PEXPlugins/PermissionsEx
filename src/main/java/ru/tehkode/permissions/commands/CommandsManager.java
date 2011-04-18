/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.commands;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.command.CommandSender;
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
            if (!method.isAnnotationPresent(Command.class) || !method.isAccessible()) {
                continue;
            }

            Command cmdAnotation = method.getAnnotation(Command.class);

            Map<CommandSyntax, CommandBinding> commandListeners = listeners.get(cmdAnotation.name());
            if(commandListeners == null){
                commandListeners = new HashMap<CommandSyntax, CommandBinding>();
                listeners.put(cmdAnotation.name(), commandListeners);
            }

            commandListeners.put(new CommandSyntax(cmdAnotation.syntax()), new CommandBinding(listener, method));
        }
    }

    public boolean execute(CommandSender sender, org.bukkit.command.Command command, String[] args) {
        Map<CommandSyntax, CommandBinding> callMap = this.listeners.get(command.getName());

        if(callMap == null){ // No commands registred
            return false;
        }

        CommandBinding selectedBinding = null;
        int argumentsLength = 0;
        String arguments = implodeArgs(args);

        for(Entry<CommandSyntax, CommandBinding> entry : callMap.entrySet()){
            CommandSyntax syntax = entry.getKey();

            logger.info("Matching " + arguments + " to " + syntax.getRegexp());

            if(!syntax.isMatch(arguments)){
                continue;
            }

            if(selectedBinding != null && syntax.getRegexp().length() < argumentsLength){ // match, but there already more fitted variant
                continue;
            }

            CommandBinding binding = entry.getValue();
            binding.setParams(syntax.getMatchedArguments(arguments));
            selectedBinding = binding;
        }

        if(selectedBinding == null){ // there is fitting handler
            return false;
        }

        try {
            selectedBinding.call(this.plugin, sender, selectedBinding.getParams());
        } catch (RuntimeException e){
            Logger.getLogger("Minecraft").severe("There is bogus command handler for "+command.getName() + " command. (Is appopriate plugin is update?)");
        }

        return true;
    }

    protected String implodeArgs(String[] args) {
        String arguments = "";
        for (int i = 0; i < args.length; i++) {
            arguments += " " + args[i];
        }
        return arguments;
    }

    protected class CommandSyntax {

        protected String originalSyntax;
        protected String regexp;
        protected List<String> arguments = new LinkedList<String>();

        public CommandSyntax(String syntax){
            this.originalSyntax = syntax;

            this.regexp = this.prepareSyntaxRegexp(syntax);
        }

        public String getRegexp() {
            return regexp;
        }

        private String prepareSyntaxRegexp(String syntax){
            String expression = syntax;

            Matcher argMatcher = Pattern.compile("((?:[\\s]+)(\\<|\\[)([^\\>]+)(?:\\>|\\]))").matcher(expression);

            logger.info("Converting \"" + syntax + "\"");

            while(argMatcher.find()){
                arguments.add(argMatcher.group(2));
                String regExpression = "((?:\\\")[^\\\"]+(?:\\\")|[^\\s]+)";

                logger.info("Found argument \"" + argMatcher.group()+"\"");
                if(argMatcher.group(1).equals("[")){
                    regExpression += "?"; //make group optional
                }

                expression = expression.replace(argMatcher.group(), "(?:[\\s]+)?"+regExpression+"(?:[\\s]+)?");
            }

            logger.info("Converted to \"" + expression + "\"");

            return expression;
        }

        public boolean isMatch(String str){
            return str.matches(this.regexp);
        }

        public Map<String, String> getMatchedArguments(String str){
            Matcher argMatcher = Pattern.compile(this.regexp).matcher(str);

            Map<String, String> matchedArguments = new HashMap<String, String>();

            int index = 0;
            while(argMatcher.find()){
                String argumentValue = argMatcher.group();

                if(argumentValue.startsWith("\"") && argumentValue.endsWith("\"")){ // Trim boundary colons
                    argumentValue = argumentValue.substring(1, -1);
                }

                matchedArguments.put(this.arguments.get(index++), argumentValue);
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
