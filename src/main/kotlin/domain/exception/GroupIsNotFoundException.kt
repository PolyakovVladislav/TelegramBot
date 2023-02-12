package domain.exception

import java.lang.Exception

open class GroupIsNotFoundException(
    message: String
): Exception(message)
