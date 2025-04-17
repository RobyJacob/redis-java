import java.security.cert.CRL;

public class Utility {
    private static final String CRLF = "\r\n";

    public static String convertToResp(String message, RespParser.Operand respType) {
        StringBuilder builder = new StringBuilder();

        switch (respType) {
            case BULKSTRING:
                builder.append(respType)
                    .append(message.length())
                    .append(CRLF)
                    .append(message)
                    .append(CRLF);
                break;
            
            case STRING:
                builder.append(respType)
                    .append(message)
                    .append(CRLF);
                break;
            
            case ERROR:
                builder.append(RespParser.Operand.BULKSTRING)
                    .append(respType)
                    .append(message)
                    .append(CRLF);
                break;

            case ARRAY:
                String[] strSplit = message.split(" ");
                builder.append(respType)
                    .append(strSplit.length)
                    .append(CRLF);
                
                for (String str : strSplit) {
                    builder.append(convertToResp(str, RespParser.Operand.BULKSTRING));
                }
                break;
            
            default:
                throw new IllegalArgumentException("Unsupported RESP type: %s".formatted(respType));
        }

        return builder.toString();
    }
}
