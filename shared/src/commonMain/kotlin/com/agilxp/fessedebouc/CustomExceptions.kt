package com.agilxp.fessedebouc

class BadRequestException(override val message: String): Exception()

class UnauthorizedException(override val message: String): Exception()

class ConflictException(override val message: String): Exception()

class UnknownServerException(override val message: String): Exception()