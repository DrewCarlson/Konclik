package de.dbaelz.konclik

fun parseArgs(command: Command, args: List<String>): ParseResult {
    val providedPositionalArguments = mutableMapOf<String, String>()
    val providedOptions = mutableMapOf<String, List<String>>()

    var positionalArgumentsHandled = false
    var argsHandledCounter = 0

    val argsListIterator = args.listIterator()
    while (argsListIterator.hasNext()) {
        val arg = argsListIterator.next()

        if (arg.startsWith("-")) {
            // It's an option (starting with "-" or "--")
            val option = command.getOptionByName(arg)

            // Option found: No more position arguments from here on
            positionalArgumentsHandled = true

            if (option == null) {
                return ParseResult.Error("ERROR: The command has no option \"$arg\"")
            }

            when (option) {
                is Parameter.Option.Switch -> providedOptions.put(option.name, emptyList())
                is Parameter.Option.Value -> {
                    val requiredArgs = option.numberArgs - option.defaults.size
                    if (enoughArgsProvided(args.listIterator(argsListIterator.nextIndex()), requiredArgs)) {
                        val values = mutableListOf<String>()
                        while (argsListIterator.hasNext()) {
                            val current = argsListIterator.next()
                            if (current.startsWith("-")) {
                                // Next option detected: Move the cursor backwards so the arg is evaluated again
                                argsListIterator.previous()
                                break
                            } else {
                                values.add(current)
                            }
                        }
                        if (values.size < option.numberArgs) {
                            // There are less args provided then the option requires
                            // Simply fill up with the defaults beginning with the first one
                            values.addAll(option.defaults.subList(0, option.numberArgs - values.size))
                        }
                        providedOptions[option.name] = values
                    } else {
                        return ParseResult.Error("ERROR: Not enough args provided for option ${option.name}")
                    }
                }
            }

        } else if (!positionalArgumentsHandled) {
            // It's a positional argument
            if (argsHandledCounter < command.arguments.size) {
                // Add with key = argument.name, value = arg
                providedPositionalArguments[command.arguments[argsHandledCounter].name] = arg
                argsHandledCounter++
            } else {
                return ParseResult.Error("ERROR: More positional arguments provided than expected. Argument: $arg")
            }
        } else {
            return ParseResult.Error("ERROR: The positional arguments must precede the options. Argument: $arg")
        }
    }
    return ParseResult.Parameters(providedPositionalArguments, providedOptions)
}

private fun enoughArgsProvided(iterator: ListIterator<String>, requiredArgs: Int): Boolean {
    if (requiredArgs <= 0) return true

    var remaining = requiredArgs
    while (iterator.hasNext()) {
        if (iterator.next().startsWith("-")) {
            // Next option detected. Check if enough args provided
            return remaining == 0
        } else {
            remaining--
        }
    }
    return remaining == 0
}