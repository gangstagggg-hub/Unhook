package com.unhook.app

/**
 * Teller bruk for tidsavbruddet. Ren Kotlin uten Android-avhengigheter, så logikken kan testes.
 *
 * - Apper uten egen tid deler én teller mot hovedtiden.
 * - En app med egen tid har sin egen teller mot sin egen grense.
 * - Når en av grensene nås, sperres alle appene i [lockMs].
 * - Tellerne nullstilles når alle appene har vært lukket i [BREAK_MS], og etter sperren.
 *   Lukker og åpner du appene igjen raskere enn det, teller tiden videre. Da blir appene
 *   likevel sperret når grensen nås.
 */
class TimeoutTracker {
    companion object {
        /** Hvor lenge alle appene må være lukket før brukstiden regnes som nullstilt. */
        const val BREAK_MS = 60_000L
    }

    private var sharedMs = 0L
    private val ownMs = HashMap<String, Long>()
    private var lastUsedAt = 0L
    private var lockedUntil = 0L

    fun reset() {
        sharedMs = 0L
        ownMs.clear()
        lastUsedAt = 0L
        lockedUntil = 0L
    }

    /**
     * Kalles mens en av appene på lista er i forgrunnen.
     * @param ownLimitMs egen tid for denne appen, eller 0 for å bruke hovedtiden
     * @param lockMs hvor lenge appene er sperret når en grense nås
     * @return tidspunktet appene er sperret til, eller 0 hvis de ikke er sperret
     */
    fun update(pkg: String, now: Long, deltaMs: Long, mainLimitMs: Long, ownLimitMs: Long, lockMs: Long): Long {
        if (lockedUntil != 0L) {
            if (now < lockedUntil) return lockedUntil
            lockedUntil = 0L
            sharedMs = 0L
            ownMs.clear()
        }

        if (lastUsedAt != 0L && now - lastUsedAt >= BREAK_MS) {
            sharedMs = 0L
            ownMs.clear()
        }
        lastUsedAt = now

        val reached: Boolean
        if (ownLimitMs > 0L) {
            val used = (ownMs[pkg] ?: 0L) + deltaMs
            ownMs[pkg] = used
            reached = used >= ownLimitMs
        } else {
            sharedMs += deltaMs
            reached = sharedMs >= mainLimitMs
        }

        if (reached) {
            lockedUntil = now + lockMs
            return lockedUntil
        }
        return 0L
    }
}
