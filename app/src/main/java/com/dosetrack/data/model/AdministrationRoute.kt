package com.dosetrack.data.model

/** How a medication is taken. Only injected medications are logged with a site on the body diagram. */
enum class AdministrationRoute(val label: String) {
    INJECTION("Injection"),
    ORAL("Oral"),
    TOPICAL("Topical"),
    INHALED("Inhaled"),
    OTHER("Other"),
    ;

    val isInjected: Boolean
        get() = this == INJECTION
}
