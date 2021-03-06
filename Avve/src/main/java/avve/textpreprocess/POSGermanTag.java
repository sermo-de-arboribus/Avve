package avve.textpreprocess;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Copied from https://stackoverflow.com/questions/4570751/what-tag-set-is-used-in-opennlps-german-maxent-model
 * Just for reference purposes
 * 
 * @author "Prine"
 *
 */
public enum POSGermanTag
{
    ADJA("Attributives Adjektiv"),
    ADJD("Adverbiales oder prädikatives Adjektiv"),
    ADV("Adverb"),
    APPR("Pr�position; Zirkumposition links"),
    APPRART("Pr�position mit Artikel"),
    APPO("Postposition"),
    APZR("Zirkumposition rechts"),
    ART("Bestimmer oder unbestimmer Artikel"),
    CARD("Kardinalzahl"),
    FM("Fremdsprachichles Material"),
    ITJ("Interjektion"),
    KOUI("unterordnende Konjunktion mit zu und Infinitiv"),
    KOUS("unterordnende Konjunktion mit Satz"),
    KON("nebenordnende Konjunktion"),
    KOKOM("Vergleichskonjunktion"),
    NN("normales Nomen"),
    NE("Eigennamen"),
    PDS("substituierendes Demonstrativpronomen"),
    PDAT("attribuierendes Demonstrativpronomen"),
    PIS("substituierendes Indefinitpronomen"),
    PIAT("attribuierendes Indefinitpronomen ohne Determiner"),
    PIDAT("attribuierendes Indefinitpronomen mit Determiner"),
    PPER("irreflexives Personalpronomen"),
    PPOSS("substituierendes Possessivpronomen"),
    PPOSAT("attribuierendes Possessivpronomen"),
    PRELS("substituierendes Relativpronomen"),
    PRELAT("attribuierendes Relativpronomen"),
    PRF("reflexives Personalpronomen"),
    PROAV("Pronominaladverb"),
    PWS("substituierendes Interrogativpronomen"),
    PWAT("attribuierendes Interrogativpronomen"),
    PWAV("adverbiales Interrogativ- oder Relativpronomen"),
    PAV("Pronominaladverb"),
    PTKZU("zu vor Infinitiv"),
    PTKNEG("Negationspartikel"),
    PTKVZ("abgetrennter Verbzusatz"),
    PTKANT("Antwortpartikel"),
    PTKA("Partikel bei Adjektiv oder Adverb"),
    TRUNC("Kompositions-Erstglied"),
    VVFIN("finites Verb, voll"),
    VVIMP("Imperativ, voll"),
    VVINF("Infinitiv"),
    VVIZU("Infinitiv mit zu"),
    VVPP("Partizip Perfekt"),
    VAFIN("finites Verb, aux"),
    VAIMP("Imperativ, aux"),
    VAINF("Infinitiv, aux"),
    VAPP("Partizip Perfekt, aux"),
    VMFIN("finites Verb, modal"),
    VMINF("Infinitiv, modal"),
    VMPP("Partizip Perfekt, modal"),
    XY("Nichtwort, Sonderzeichen"),
    UNDEFINED("Nicht definiert, zb. Satzzeichen");

    private final String desc;

    private static final Map<String, POSGermanTag> nameToValueMap = new HashMap<String, POSGermanTag>();

    static
    {
        for (POSGermanTag value : EnumSet.allOf(POSGermanTag.class))
        {
            nameToValueMap.put(value.name(), value);
        }
    }

    public static POSGermanTag forName(String name)
    {
        return nameToValueMap.get(name);
    }

    private POSGermanTag(String desc)
    {
        this.desc = desc;
    }

    public String getDesc()
    {
        return this.desc;
    }
}