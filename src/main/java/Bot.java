import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if(update.hasMessage()) {
            if(update.getMessage().getText().equals(BotConstant.MESSAGE_START)) {
                execute(greetingMessage(message));
                addButtons(BotConstant.LANGUAGES, BotConstant.CHOOSE_LANGUAGE, update.getMessage().getChatId());
            }else if(update.getMessage().getText().equals(BotConstant.MESSAGE_STOP)){

                execute(stop(message));
            }
            else {
                execute(errorMessage(message));
            }
        }
        else if(update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String[] word = data.split("-");
            switch (word[0]) {
                case "language":
                    showCategory(word[1], chatId);
                    break;
                case "category":
                    showQuestions(word[1], word[2], chatId);
                    break;
                case "question":

                    workInQuestion(word[1], word[2], word[3], word[4], chatId);
                    break;
            }
            if(data.contains("back")) {
                back(word[1], chatId);
            }
        }
    }


    ////////////////////////////////////////////

    private void showCategory(String language, Long chatId) throws IOException {
        String path = String.format(BotConstant.CATEGORY, language);
        addButtons(path, BotConstant.CHOOSE_CATEGORY, chatId);
    }

    private void showQuestions(String language, String category, Long chatId) throws IOException {

                String questionsPack = String.format(BotConstant.QUESTIONS, language, category);
                List<BotQuestion> questions = objectMapper.readValue(new File(questionsPack), new TypeReference<List<BotQuestion>>() {
                });
                showNextQuestion(questions, 1, chatId);


    }

    private void workInQuestion(String language, String topic, String messageId, String button, Long chatId) throws IOException {
        int id = Integer.parseInt(messageId);
        String questionsPack = String.format(BotConstant.QUESTIONS , language, topic);
        List<BotQuestion> questions = objectMapper.readValue(new File(questionsPack), new TypeReference<List<BotQuestion>>() {});
        switch (button) {
            case "answer":
                showAnswer(questions, id, chatId);
                break;
            case "next":
                showNextQuestion(questions, id + 1, chatId);
                break;
        }
    }

    private void back(String data, Long chatId) throws IOException {
        if (data.equals("languages")) {
            addButtons(BotConstant.LANGUAGES, BotConstant.CHOOSE_LANGUAGE, chatId);
        } else {
            String path = String.format(BotConstant.CATEGORY, data);
            addButtons(path, BotConstant.CHOOSE_CATEGORY, chatId);
        }
    }


//////////////////////////////////////////

    private void sendMessage(String text, InlineKeyboardMarkup keyboard, Long chatId) {
        try {
            execute(new SendMessage().setChatId(chatId).setText(text).setReplyMarkup(keyboard));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void addButtons(String path, String text, Long chatId) throws IOException {
        List<BotMenu> buttons = objectMapper.readValue(new File(path), new TypeReference<List<BotMenu>>() {});
        InlineKeyboardMarkup inlineKeyboardMarkup = createKeyboard(buttons);
        sendMessage(text, inlineKeyboardMarkup, chatId);
    }

    private InlineKeyboardMarkup createKeyboard(List<BotMenu> buttons) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        buttons.forEach(element -> {
            List<InlineKeyboardButton> row = Collections.singletonList(new InlineKeyboardButton().setText(element.getName())
                    .setCallbackData(element.getCallbackData()));
            rowList.add(row); });
        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
    private SendMessage greetingMessage (Message message){
        SendMessage response = new SendMessage();
        response.setText(BotConstant.GREETING_MESSAGE);
        response.setChatId(message.getChatId());//отправляет в нужный чат
        response.setReplyMarkup(keyboardMenu());
        return response;
    }
    private SendMessage errorMessage (Message message){
        SendMessage response = new SendMessage();
        response.setText(BotConstant.ERROR_INPUT);
        response.setChatId(message.getChatId());//отправляет в нужный чат
        response.setReplyMarkup(keyboardMenu());
        return response;
    }

    private SendMessage stop (Message message){
        SendMessage response = new SendMessage();
        response.setText(BotConstant.StopBot);
        response.setChatId(message.getChatId());
        response.setReplyMarkup(keyboardStart());
        return response;
    }
    private ReplyKeyboardMarkup keyboardStart(){
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);//параметр вывод определенному пользователю или нескольким
        markup.setResizeKeyboard(true);//авто подгонка клавиатури под кол. кнопок
        markup.setOneTimeKeyboard(false);//скривать клавиатуры после использования или нет

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BotConstant.MESSAGE_START);

        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);

        markup.setKeyboard(rows);
        return markup;
    }

    private ReplyKeyboardMarkup keyboardMenu(){
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);//параметр вывод определенному пользователю или нескольким
        markup.setResizeKeyboard(true);//авто подгонка клавиатури под кол. кнопок
        markup.setOneTimeKeyboard(false);//скривать клавиатуры после использования или нет

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BotConstant.MESSAGE_STOP);
        List<KeyboardRow> rows = new ArrayList<>();
        rows.add(row1);

        markup.setKeyboard(rows);
        return markup;
    }

    ///////////////////////////////////////////////////////

    private void showNextQuestion(List<BotQuestion> questions, int id, Long chatId) throws IOException {
        List<BotMenu> buttons = objectMapper.readValue(new File(BotConstant.QUESTION_BUTTONS), new TypeReference<List<BotMenu>>() {});
        if(id > questions.size()) {
            return;
        }
        BotQuestion target = getTargetQuestion(questions, id);
        buttons.forEach(button -> button.setCallbackData(target.getCallbackData() + button.getCallbackData()));
        InlineKeyboardMarkup inlineKeyboardMarkup = createKeyboard(buttons);
        sendMessage(target.getQuestion(), inlineKeyboardMarkup, chatId);
    }

    private void showAnswer(List<BotQuestion> questions, int id, Long chatId) throws IOException {
        List<BotMenu> buttons = objectMapper.readValue(new File(BotConstant.ANSWER_BUTTONS), new TypeReference<List<BotMenu>>() {});
        if(id > questions.size()) {
            return;
        }
        BotQuestion target = getTargetQuestion(questions, id);
        buttons.forEach(button -> button.setCallbackData(target.getCallbackData() + button.getCallbackData()));
        InlineKeyboardMarkup inlineKeyboardMarkup = createKeyboard(buttons);
        sendMessage(target.getAnswer(), inlineKeyboardMarkup, chatId);
    }

    private BotQuestion getTargetQuestion(List<BotQuestion> questions, int id) {
        return questions.stream()
                .filter(question -> question.getId() == id).findFirst()
                .orElseThrow(RuntimeException::new);
    }






    public String getBotUsername() {
        return BotConstant.BOT_NAME;
    }

    public String getBotToken() {
        return BotConstant.BOT_TOKEN;
    }
}
