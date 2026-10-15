package com.resume.builder.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide scope for small jobs that must finish even when the screen that started them
 * closes right away (for example granting the ads-free reward after the sheet has gone).
 */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO)
