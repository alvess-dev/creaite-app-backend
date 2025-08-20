package domain.usecase
import com.example.appcreaite.domain.model
import core.validation.Passwordvalidator
import data.AuthRepository.AuthRepository
import domain.model.User


class LoginUseCase(private val repository: AuthRepository) {
    suspend fun execute(email: String, password: String): Result<User> {
        if (!Passwordvalidator.isValid(password)) {
            return Result.failure(Exception("Senha inválida"))
        }
        return repository.login(email, password)
    }
}

private fun Passwordvalidator.Companion.isValid(password: String): Boolean {
    TODO("Not yet implemented")
}
