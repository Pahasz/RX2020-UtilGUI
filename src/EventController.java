import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class EventController {

    HashMap<String, JsonEvent> eventMap = new HashMap<>();
    HashMap<String, String> fileMap = new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();
    private JsonEvent event;

    ComboBox<String> eventComboBox, fileComboBox;

    CheckBox isRoot;
    TextField titleTextField, nameTextField;

    ArrayList<TextArea> descList = new ArrayList<>();
    ArrayList<ChoiceLink> choiceList = new ArrayList<>();
    ArrayList<ArrayList<TextField>> actionList = new ArrayList<>();
    public ArrayList<ArrayList<TextField>> restrictionList = new ArrayList<>();

    private File loadedFile;

    void reset(){
        descList = new ArrayList<>();
        choiceList = new ArrayList<>();
        actionList = new ArrayList<>();
    }

    void loadAllJsonFiles(String path){
        File currPath = new File(new File(path).getAbsolutePath());

        File[] list = currPath.listFiles();
        if(list != null) {
            for (File file : list) {
                if (file.isDirectory()) {
                    loadAllJsonFiles(file.getAbsolutePath());
                } else {
                    if (file.getPath().endsWith(".json"))
                        fileMap.put(file.getName(), file.getAbsolutePath());
                }
            }
        }
    }

    void loadEventList(String fileName) {
        eventMap = new HashMap<>(); //Reset the event map.
        loadedFile = new File(fileName); //Set the loaded file.

        if (!loadedFile.isDirectory() && loadedFile.exists() && loadedFile.length() > 0){
            try {
                JsonEvent[] events = mapper.readValue(loadedFile, JsonEvent[].class);
                for (JsonEvent evt : events)
                    eventMap.put(evt.name, evt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        eventComboBox.setItems(FXCollections.observableArrayList(Helper.asSortedList(eventMap.keySet())));
    }

    /**
     * Replaces the currently selected Event in the dropdown with the one currently on screen.
     * This means that the selected one will be removed and the current one will be saved.
     */
    void replace(){
        if(!eventComboBox.getValue().isEmpty() && !nameTextField.getText().isEmpty() && !titleTextField.getText().isEmpty()) {
            save();
            eventMap.remove(eventComboBox.getValue());
            writeToJson();
            loadEventList(fileMap.get(fileComboBox.getValue()));
        }
    }

    /**
     *
     */
    void save(){
        event = new JsonEvent();
        event.root = isRoot.isSelected();
        event.title = titleTextField.getText();
        event.name = nameTextField.getText();

        if(event.title.isEmpty() || event.name.isEmpty())
            return;

        /*
         * Record all the descriptions. Only use child 3+
         */
        ArrayList<TextArea> descList = this.descList;
        event.description = new String[descList.size()];
        for(int i=0;i<descList.size();i++) //Record all the text.
            event.description[i] = descList.get(i).getText();


        /*
         * Record all the choices. Complicated much very.
         */
        int i=0;
        ArrayList<ChoiceLink> choiceLinkList = this.choiceList;

        //If the link is only 1 and contains empty text, make an empty array.
        if(choiceLinkList.size() == 1 && choiceLinkList.get(0).choiceText.getText().isEmpty()) {
            event.choices = new String[0];
            event.restrictions = new String[0];
        }else{
            event.choices = new String[choiceLinkList.size()];
            event.restrictions = new String[choiceLinkList.size()];
        }

        for(ChoiceLink link : choiceLinkList){
            ArrayList<String> outcomes = new ArrayList<>();
            ArrayList<Float> chances = new ArrayList<>();

            if(event.choices.length > i)
                event.choices[i] = link.choiceText.getText();

            if(event.restrictions.length > i)
                event.restrictions[i] = link.restrictionText.getText();

            for(TextField outcomeField : link.outcomeList){
                if(!outcomeField.getText().isEmpty())
                    outcomes.add(outcomeField.getText());
            }

            if(!(link.chanceList.size() == 1 && link.chanceList.get(0).getText().isEmpty())) {
                for (TextField chanceField : link.chanceList) {
                    float value = 0;
                    try {
                        value = Float.parseFloat(chanceField.getText());
                    } catch (NumberFormatException e) {
//                    e.printStackTrace();
                    }

                    chances.add(value);
                }
            }

            event.outcomes.add(outcomes);
            event.chances.add(chances);

            i++;
        }

        event.resultingAction = new ArrayList<>();
        if(actionList.size() > 0) {
            for (ArrayList<TextField> list : actionList) {
                ArrayList<String> paramList = new ArrayList<>();
                event.resultingAction.add(paramList);
                paramList.addAll(list.stream().map(TextField::getText).collect(Collectors.toList()));
            }
        }

        eventMap.put(event.name, event);
    }

    void writeToJson(){
        try {
            List<JsonEvent> list = Helper.asSortedList(eventMap.values(), (e1, e2) -> e1.title.compareToIgnoreCase(e2.title));
           mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writerWithDefaultPrettyPrinter().writeValue(loadedFile, list);

//            String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class JsonEvent{
        public Boolean root = false;
        public String title, name;
        public String[] description = new String[0];
        public String[] choices = new String[0];
        public String[] restrictions = new String[0];
        public ArrayList<ArrayList<String>> outcomes = new ArrayList<>();
        public ArrayList<ArrayList<Float>> chances = new ArrayList<>();
        public ArrayList<ArrayList<String>> resultingAction = new ArrayList<>();
    }
}
