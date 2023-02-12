package domain.exception

import java.lang.Exception

open class UnexpectedGroupType(
    message: String
): Exception(message)
