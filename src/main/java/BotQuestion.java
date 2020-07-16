import lombok.Data;

@Data
public class BotQuestion {

    private int id;
    private String question;
    private String answer;
    private String callbackData;
}