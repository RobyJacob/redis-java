public class Utility {
    private static final String CRLF = "\r\n";

    public static String convertToResp(String str, RespParser.Operand outputType) {
        StringBuilder builder = new StringBuilder();

        switch (outputType) {
            case BULKSTRING:
                builder.append(outputType)
                    .append(str.length())
                    .append(CRLF)
                    .append(str)
                    .append(CRLF);
                break;
            
            case STRING:
                builder.append(outputType)
                    .append(str)
                    .append(CRLF);
                break;
            
            case ERROR:
                builder.append(RespParser.Operand.BULKSTRING)
                    .append(outputType)
                    .append(str)
                    .append(CRLF);
                break;
            
            default:
                throw new IllegalArgumentException("Unsupported RESP type: %s".formatted(outputType));
        }

        return builder.toString();
    }
}
