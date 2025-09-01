package core.validation

object PasswordValidator {
    fun isValid(password: String): Boolean {
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { "!@#\$%^&*()_+".contains(it) }

        return password.length >= 8 && hasUpper && hasDigit && hasSpecial
    }

    fun getValidationMessage(password: String): String {
        return when {
            password.length < 8 -> "A senha deve ter pelo menos 8 caracteres."
            !password.any { it.isUpperCase() } -> "A senha deve conter letras maiúsculas."
            !password.any { it.isDigit() } -> "A senha deve conter números."
            !password.any { "!@#\$%^&*()_+".contains(it) } -> "A senha deve conter caracteres especiais."
            else -> "Senha válida."
        }
    }
}

class Passwordvalidator {
    companion object

}