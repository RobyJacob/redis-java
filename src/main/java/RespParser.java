import java.util.*;

public class RespParser {
    enum Operand {
        ARRAY('*'),
        BULKSTRING('$'),
        INTEGER(':'),
        ERROR('-'),
        STRING('+');

        private Character typeChar;

        Operand(Character typeChar) {
            this.typeChar = typeChar;
        }

        @Override
        public String toString() {
            return typeChar.toString();
        }
    }

    private Stack<Character> operator;
    private Stack<Integer> size;
    private final String CRLF = "\\r\\n";

    RespParser() {
        operator = new Stack<>();
        size = new Stack<>();
    }

    void parse(String respString) {
        for (String str : respString.split(CRLF)) {
            if (Arrays.stream(Operand.values()).anyMatch(op -> op.typeChar.equals(str.charAt(0)))) {
                operator.push(str.charAt(0));
                size.push(Integer.parseInt(str.substring(1)));
            } else {
                if (!size.peek().equals(str.length()))
                    throw new RuntimeException("Size does not match");

                if (Commands.isCommand(str))
                    Commands.add(str);
                else
                    Commands.addArgs(List.of(str));
            }
        }

        while (!operator.isEmpty() && !size.isEmpty()) {
            operator.pop();
            size.pop();
        }

        if (!operator.isEmpty() || !size.isEmpty())
            throw new RuntimeException("Request is invalid");
    }

    String convertToResp(String str) {
        StringBuilder builder = new StringBuilder();

        builder.append(Operand.BULKSTRING)
                .append(str.length())
                .append(CRLF)
                .append(str)
                .append(CRLF);

        return builder.toString();
    }
}
