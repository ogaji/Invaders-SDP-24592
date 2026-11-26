package shop;

/** The outcome of attempting to purchase a shop product. */
public enum PurchaseResult {
    SUCCESS,
    INVALID_PRODUCT_ID,
    PREREQUISITE_NOT_MET,
    UNKNOWN_PRODUCT,
    INSUFFICIENT_FUNDS,
    /** The product is owned or available by default. */
    ALREADY_AVAILABLE,
    UNLOCK_FAILED
}
