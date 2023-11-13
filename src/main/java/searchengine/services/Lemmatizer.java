package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
public class Lemmatizer {
    private final List<String> WRONG_TYPES = List.of("ПРЕДЛ", "СОЮЗ", "МЕЖД", "ВВОДН", "ЧАСТ", "МС", "CONJ", "PART", "NOUN");
    private final LuceneMorphology russianMorphology;
    private final LuceneMorphology englishMorphology;

    public Lemmatizer() {
        try {
            russianMorphology = new RussianLuceneMorphology();
            englishMorphology = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Integer> lemmatize(String text) {
        HashMap<String, Integer> result = new HashMap<>();

        List<String> words = Arrays.stream(text.toLowerCase().split("\\s+|[^\\p{L}]+")).toList();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            if (checkType(word)) {
                continue;
            }

            List<String> normalWords;

            if (russianMorphology.checkString(word)) {
                normalWords = russianMorphology.getNormalForms(word);
            } else {
                normalWords = englishMorphology.getNormalForms(word);
            }

            if (normalWords.isEmpty()) {
                continue;
            }

            for (String normalWord : normalWords) {
                result.merge(normalWord, 1, Integer::sum);
            }
        }
        return result;
    }

    private boolean checkType(String word) {
        List<String> wordBaseForm;

        if (russianMorphology.checkString(word)) {
            wordBaseForm = russianMorphology.getMorphInfo(word);
        } else if (englishMorphology.checkString(word)) {
            wordBaseForm = englishMorphology.getMorphInfo(word);
        } else {
            return true;
        }

        for (String type : WRONG_TYPES) {
            if (wordBaseForm.toString().contains(type)) {
                return true;
            }
        }
        return false;
    }

    public String clearText(String text) {
        return Jsoup.parse(text).body().text();
    }
}
