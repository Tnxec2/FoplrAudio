package com.kontranik.foplraudio.ui.player

import android.os.Bundle
import androidx.media3.session.SessionCommand

object CustomCommands {

    const val ACTION_TOGGLE_PAUSE_AT_END = "com.kontranik.foplraudio.ACTION_TOGGLE_PAUSE_AT_END"
    const val PARAM_PAUSE_AT_END = "PARAM_PAUSE_AT_END"

    // Befehl zum Abfragen des Status
    const val ACTION_GET_PAUSE_AT_END_STATUS = "com.kontranik.foplraudio.ACTION_GET_PAUSE_AT_END_STATUS"
    // Schlüssel für den Rückgabewert im Bundle
    const val RESULT_PAUSE_AT_END_STATUS = "RESULT_PAUSE_AT_END_STATUS"

    // SessionCommand-Objekte
    val COMMAND_TOGGLE_PAUSE_AT_END = SessionCommand(ACTION_TOGGLE_PAUSE_AT_END, Bundle.EMPTY)
    val COMMAND_GET_PAUSE_AT_END_STATUS = SessionCommand(ACTION_GET_PAUSE_AT_END_STATUS, Bundle.EMPTY)
}
