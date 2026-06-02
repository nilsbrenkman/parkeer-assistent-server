package nl.parkeerassistent.service

/**
 * Identifies a single operation within a service for logging and metrics.
 *
 * Each service defines its own `Method` enum implementing this interface; the
 * resulting (service, method) pair is used by [nl.parkeerassistent.Metrics] as
 * consistent labels on log lines and Prometheus counters.
 */
interface ServiceMethod {
    /** The stable name of the owning service, e.g. `"Parking"` or `"User"`. */
    fun service(): String
    /** The name of this specific method within the service, e.g. `"Start"`. */
    fun method(): String
}