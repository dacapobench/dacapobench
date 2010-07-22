package org.dacapo.instrument;

public final class LogTags {

	public static final String LOG_PREFIX_START                     = "START";
	public static final String LOG_PREFIX_STOP                      = "STOP";

	public static final String LOG_PREFIX_ALLOCATION                = "HA";
	public static final String LOG_PREFIX_FREE                      = "HF";
	public static final String LOG_PREFIX_POINTER                   = "HC";
	public static final String LOG_PREFIX_STATIC_POINTER            = "HS";

	public static final String LOG_PREFIX_CLASS_PREPARE             = "LD";
	public static final String LOG_PREFIX_METHOD_PREPARE            = "LM";
	public static final String LOG_PREFIX_CLASS_INITIALIZATION      = "CI";

	public static final String LOG_PREFIX_THREAD_START              = "TS";
	public static final String LOG_PREFIX_THREAD_STOP               = "TE";
	public static final String LOG_PREFIX_THREAD_STATUS             = "TA";
	public static final String LOG_PREFIX_THREAD_TIME               = "TT";              

	public static final String LOG_PREFIX_METHOD_ENTER              = "CS";
	public static final String LOG_PREFIX_METHOD_EXIT               = "CE";

	public static final String LOG_PREFIX_MONITOR_ACQUIRE           = "MS";
	public static final String LOG_PREFIX_MONITOR_RELEASE           = "ME";
	public static final String LOG_PREFIX_MONITOR_NOTIFY            = "MN";
	public static final String LOG_PREFIX_MONITOR_CONTENTED_ENTER   = "MC";
	public static final String LOG_PREFIX_MONITOR_CONTENTED_ENTERED = "Mc";
	public static final String LOG_PREFIX_MONITOR_WAIT              = "MW";
	public static final String LOG_PREFIX_MONITOR_WAITED            = "Mw";

	public static final String LOG_PREFIX_VOLATILE                  = "VF";
	public static final String LOG_PREFIX_VOLATILE_ACCESS           = "VA";

	public static final String LOG_PREFIX_EXCEPTION                 = "XT";
	public static final String LOG_PREFIX_EXCEPTION_CATCH           = "XC";

	public static final String LOG_PREFIX_TIME                      = "TM";

	public static final String LOG_PREFIX_GC                        = "GC";
	public static final String LOG_PREFIX_HEAP_REPORT               = "HR";

	public static final String LOG_PREFIX_CALL_CHAIN_START          = "ES";
	public static final String LOG_PREFIX_CALL_CHAIN_FRAME          = "EF";
	public static final String LOG_PREFIX_CALL_CHAIN_STOP           = "EE";

}
