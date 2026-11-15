package com.santiagorey.devflow.ai

/** Reto de programación del día generado por la IA. */
data class DailyChallenge(
    val title: String,
    val statement: String,
    val hint: String,
    val solution: String
) {
    companion object {
        const val HINT_MARK = "PISTA:"
        const val SOLUTION_MARK = "SOLUCIÓN:"

        /**
         * Separa la respuesta de la IA en título, enunciado, pista y solución usando las
         * marcas pedidas en el prompt. Devuelve null si falta alguna parte.
         */
        fun parse(reply: String): DailyChallenge? {
            val text = reply.trim()
            val hintAt = text.indexOf(HINT_MARK, ignoreCase = true)
            val solutionAt = text.indexOf(SOLUTION_MARK, ignoreCase = true).takeIf { it >= 0 }
                ?: text.indexOf("SOLUCION:", ignoreCase = true)
            if (hintAt <= 0 || solutionAt <= hintAt) return null

            val head = text.substring(0, hintAt).trim()
            val title = head.lineSequence().first().trim().trimStart('#', '*', ' ').trimEnd('*', ' ')
            val statement = head.substringAfter('\n', "").trim()
            val hint = text.substring(hintAt + HINT_MARK.length, solutionAt).trim()
            val solution = text.substring(solutionAt).substringAfter(':').trim()
            if (title.isBlank() || statement.isBlank() || solution.isBlank()) return null
            return DailyChallenge(title, statement, hint, solution)
        }
    }
}
