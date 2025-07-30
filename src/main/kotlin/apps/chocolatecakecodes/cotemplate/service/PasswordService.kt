package apps.chocolatecakecodes.cotemplate.service

import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped
import java.security.SecureRandom

@ApplicationScoped
internal class PasswordService {

    companion object {
        val passwordChars = charArrayOf(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '_', '+', '@', '#', '%', '*', '&', '~'
        )
    }

    private val rng = SecureRandom()

    fun generateRandomPassword(len: Int = 24): String {
        val chars = CharArray(len) { passwordChars[rng.nextInt(len)] }
        return String(chars)
    }

    fun hashPassword(password: String): String {
        return BcryptUtil.bcryptHash(password, 14)
    }

    fun checkPassword(password: String, hash: String): Boolean {
        return BcryptUtil.matches(password, hash)
    }

}
