package com.yahoo.vespa.hosted.controller.versions;

/**
 * Maturity status of major versions in Vespa Cloud.
 *
 * @author jonmv
 */
public enum MajorVersionStatus {

    /** Only applications which opt in will get this major. */
    CURRENT,

    /** The newest such major is the default target for new applications. */
    STABLE,

    /** This major is only available to applications already on this version. */
    OUTDATED,

    /** The system will actively try to upgrade applications on this major, unless it is set in deployment spec. */
    LEGACY;

    public static MajorVersionStatus from(String value) {
        return switch (value) {
            case "CURRENT"  -> MajorVersionStatus.CURRENT;
            case "STABLE"   -> MajorVersionStatus.STABLE;
            case "OUTDATED" -> MajorVersionStatus.OUTDATED;
            case "LEGACY"   -> MajorVersionStatus.LEGACY;
            default         -> throw new IllegalArgumentException("unknown major version status '" + value + "'");
        };
    }

}
