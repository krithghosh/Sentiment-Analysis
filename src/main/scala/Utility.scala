object Utility {
  final val REG_HANDLERS = "@[A-Za-z]+"
  final val REG_LINKS = "(https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9]+\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]+\\.[^\\s]{2,})"
  final val REG_PUNCTUATIONS = "[^\\w\\s]"
  final val REG_WHITESPACES = "\\s+"
  final val REG_TRIM = "^[ \\t]+"
}