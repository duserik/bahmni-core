package org.openmrs.module.bahmniemrapi.encountertransaction.mapper;

import org.bahmni.test.builder.ConceptNumericBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.api.ConceptService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.bahmniemrapi.encountertransaction.mapper.parameters.AdditionalBahmniObservationFields;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.util.LocaleUtility;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LocaleUtility.class)

public class ETObsToBahmniObsMapperTest {

    @Mock
    ConceptService conceptService;

    ETObsToBahmniObsMapper etObsToBahmniObsMapper;
    private String person1name = "superman";
    private String person2name = "RajSingh";
    private String encounterUuid = "encounter-uuid";
    private String obsGroupUuid = "obs-group-uuid";
    private String etParentConceptClass = "Misc";
    private String etValueConceptClass = "Misc";
    private String etDataType = "N/A";
    private String etConceptShortName = null;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        etObsToBahmniObsMapper = new ETObsToBahmniObsMapper(conceptService);
        mockStatic(LocaleUtility.class);
        when(LocaleUtility.getDefaultLocale()).thenReturn(Locale.ENGLISH);

    }

    private EncounterTransaction.User createETUser(String personname) {

        EncounterTransaction.User user = new EncounterTransaction.User();
        user.setPersonName(personname);
        return user;
    }

    private EncounterTransaction.Concept createETConcept(String dataType, String etConceptClass, String shortName, String uuid) {

        EncounterTransaction.Concept etConcept = new EncounterTransaction.Concept();
        etConcept.setDataType(dataType);
        etConcept.setConceptClass(etConceptClass);
        etConcept.setShortName(shortName);
        etConcept.setUuid(uuid);
        return etConcept;
    }

    private EncounterTransaction.Observation createETObservation(String UUID, EncounterTransaction.User user, String value, EncounterTransaction.Concept concept) {
        EncounterTransaction.Observation observation = new EncounterTransaction.Observation();
        observation.setUuid(UUID);
        observation.setCreator(user);
        if (concept.getConceptClass().equals("Unknown")) {
            observation.setValue(Boolean.parseBoolean(value));
        } else if (value != null) {
            observation.setValue(value);
        }
        observation.setConcept(concept);
        return observation;
    }

    private Concept createConcept(String name, String dataType, String UUID, String conceptClass, String shortName) {

        Concept concept = new org.openmrs.module.bahmniemrapi.builder.ConceptBuilder().withName(name).withDataType(dataType).withUUID(UUID).withClass(conceptClass).withShortName(shortName).build();
        return concept;
    }

    private Concept createConceptNumeric(Integer id, String name, String conceptClass, Double highNormal, Double lowNormal) {
        return new ConceptNumericBuilder()
                .withId(id)
                .withName(name)
                .withClass(conceptClass)
                .withHiNormal(highNormal)
                .withLowNormal(lowNormal)
                .build();
    }

    @Test
    public void testMap() throws Exception {

        EncounterTransaction.User user1 = createETUser(person1name);
        EncounterTransaction.User user2 = createETUser(person2name);

        EncounterTransaction.Concept etParentConcept = createETConcept(etDataType, etParentConceptClass, etConceptShortName, null);
        EncounterTransaction.Concept etValueConcept = createETConcept(etDataType, etValueConceptClass, etConceptShortName, null);


        Concept valueConcept = createConcept("valueConcept", "text", "cuuid2", "", null);
        Concept parentConcept = createConcept("parentConcept", "N/A", null, null, null);
        parentConcept.addSetMember(valueConcept);

        EncounterTransaction.Observation observation1 = createETObservation("obs1-uuid", user1, "notes", etValueConcept);
        EncounterTransaction.Observation observation2 = createETObservation("obs2-uuid", user2, null, etParentConcept);
        observation2.setGroupMembers(asList(observation1));

        AdditionalBahmniObservationFields additionalBahmniObservationFields = new AdditionalBahmniObservationFields(encounterUuid, new Date(), new Date(), obsGroupUuid);
        BahmniObservation actualObs = etObsToBahmniObsMapper.map(observation2, additionalBahmniObservationFields, asList(parentConcept), false);

        assertEquals(person2name, actualObs.getCreatorName());
        assertEquals(encounterUuid, actualObs.getEncounterUuid());
        assertEquals(obsGroupUuid, actualObs.getObsGroupUuid());
        BahmniObservation actualValueObs = actualObs.getGroupMembers().iterator().next();
        assertEquals(person1name, actualValueObs.getCreatorName());
        assertEquals("obs2-uuid", actualValueObs.getObsGroupUuid());
    }

    @Test
    public void testMapObservationValueWithUnknownConceptShortName() throws Exception {

        EncounterTransaction.User user1 = createETUser(person1name);
        EncounterTransaction.User user2 = createETUser(person2name);

        EncounterTransaction.Concept etParentConcept = createETConcept(etDataType, "Concept Details", etConceptShortName, null);
        EncounterTransaction.Concept etValueConcept = createETConcept("text", etValueConceptClass, etConceptShortName, null);
        EncounterTransaction.Concept etUnknownConcept = createETConcept("Boolean", "Unknown", "Unknown", null);


        Concept valueConcept = createConcept("valueConcept", "text", "cuuid2", "", null);
        Concept parentConcept = createConcept("parentConcept", "N/A", null, null, null);
        parentConcept.addSetMember(valueConcept);
        Concept unknownConcept = createConcept("unknownConcept", "Boolean", "cuuid3", "Unknown", "Unknown");
        parentConcept.addSetMember(unknownConcept);


        EncounterTransaction.Observation observation1 = createETObservation("obs1-uuid", user1, "notes", etValueConcept);
        EncounterTransaction.Observation observation2 = createETObservation("obs2-uuid", user2, null, etParentConcept);
        EncounterTransaction.Observation observation3 = createETObservation("obs3-uuid", user1, "true", etUnknownConcept);
        observation2.setGroupMembers(asList(observation1, observation3));

        AdditionalBahmniObservationFields additionalBahmniObservationFields = new AdditionalBahmniObservationFields(encounterUuid, new Date(), new Date(), obsGroupUuid);
        BahmniObservation actualObs = etObsToBahmniObsMapper.map(observation2, additionalBahmniObservationFields, asList(parentConcept), true);

        assertEquals("Unknown", actualObs.getValueAsString());
        assertEquals(true, actualObs.isUnknown());
    }


    @Test
    public void testMapObservationValueToUnknownConceptFullNameWhenShortNameIsNull() throws Exception {

        EncounterTransaction.User user1 = createETUser(person1name);
        EncounterTransaction.User user2 = createETUser(person2name);

        EncounterTransaction.Concept etValueConcept = createETConcept("text", etValueConceptClass, etConceptShortName, null);
        EncounterTransaction.Concept etParentConcept = createETConcept(etDataType, "Concept Details", etConceptShortName, null);
        EncounterTransaction.Concept etUnknownConcept = createETConcept("Boolean", "Unknown", "unknownConcept", null);

        Concept parentConcept = createConcept("parentConcept", "N/A", null, null, null);
        Concept unknownConcept = createConcept("unknownConcept", "Boolean", "cuuid3", "Unknown", null);
        parentConcept.addSetMember(unknownConcept);

        EncounterTransaction.Observation observation1 = createETObservation("obs1-uuid", user1, "notes", etValueConcept);
        EncounterTransaction.Observation observation2 = createETObservation("obs2-uuid", user2, null, etParentConcept);
        EncounterTransaction.Observation observation3 = createETObservation("obs3-uuid", user1, "true", etUnknownConcept);
        observation2.setGroupMembers(asList(observation1, observation3));

        AdditionalBahmniObservationFields additionalBahmniObservationFields = new AdditionalBahmniObservationFields(encounterUuid, new Date(), new Date(), obsGroupUuid);
        BahmniObservation actualObs = etObsToBahmniObsMapper.map(observation2, additionalBahmniObservationFields, asList(parentConcept), true);

        assertEquals("unknownConcept", actualObs.getValueAsString());
        assertEquals(true, actualObs.isUnknown());
    }

    @Test
    public void testMapObservationWithValueObservationFirstAndFollowedByUnknownObservation() throws Exception {

        EncounterTransaction.User user1 = createETUser(person1name);
        EncounterTransaction.User user2 = createETUser(person2name);

        EncounterTransaction.Concept etParentConcept = createETConcept(etDataType, "Concept Details", etConceptShortName, null);
        EncounterTransaction.Concept etValueConcept = createETConcept("text", etValueConceptClass, etConceptShortName, null);
        EncounterTransaction.Concept etUnknownConcept = createETConcept("Boolean", "Unknown", "Unknown", null);

        Concept valueConcept = createConcept("valueConcept", "text", "cuuid2", "", null);
        Concept parentConcept = createConcept("parentConcept", "N/A", null, null, null);
        parentConcept.addSetMember(valueConcept);
        Concept unknownConcept = createConcept("unknownConcept", "Boolean", "cuuid3", "Unknown", "Unknown");
        parentConcept.addSetMember(unknownConcept);

        EncounterTransaction.Observation observation1 = createETObservation("obs1-uuid", user1, "notes", etValueConcept);
        EncounterTransaction.Observation observation2 = createETObservation("obs2-uuid", user2, null, etParentConcept);
        EncounterTransaction.Observation observation3 = createETObservation("obs3-uuid", user1, "false", etUnknownConcept);
        observation2.setGroupMembers(asList(observation1, observation3));

        AdditionalBahmniObservationFields additionalBahmniObservationFields = new AdditionalBahmniObservationFields(encounterUuid, new Date(), new Date(), obsGroupUuid);
        BahmniObservation actualObs = etObsToBahmniObsMapper.map(observation2, additionalBahmniObservationFields, asList(parentConcept), true);

        assertEquals("notes", actualObs.getValueAsString());
        assertEquals(false, actualObs.isUnknown());

        EncounterTransaction.Observation observation4 = createETObservation("obs3-uuid", user1, "true", etUnknownConcept);
        observation2.setGroupMembers(asList(observation1, observation4));
        actualObs = etObsToBahmniObsMapper.map(observation2, additionalBahmniObservationFields, asList(parentConcept), true);

        assertEquals("Unknown", actualObs.getValueAsString());
        assertEquals(true, actualObs.isUnknown());
    }

    @Test
    public void testSetHiNormalAndLowNormalWithBahmniObservationIfNumericConcept() {

        EncounterTransaction.User user1 = createETUser(person1name);

        EncounterTransaction.Concept etParentConcept = createETConcept(etDataType, "Concept Details", etConceptShortName, "PulseDataUuid");
        EncounterTransaction.Concept etUnknownConcept = createETConcept("Boolean", "Unknown", "Unknown", null);

        EncounterTransaction.Observation parentObservation = createETObservation("obs2-uuid", user1, null, etParentConcept);
        EncounterTransaction.Observation unknownObservation = createETObservation("obs3-uuid", user1, "true", etUnknownConcept);
        parentObservation.setGroupMembers(asList(unknownObservation));

        ConceptNumeric valueConcept = (ConceptNumeric) createConceptNumeric(1, "Pulse", "Misc", 100.0, 50.0);
        Concept parentConcept = createConcept("Pulse Data", "N/A", "PulseDataUuid", null, null);
        parentConcept.addSetMember(valueConcept);
        Concept unknownConcept = createConcept("Unknown", "Boolean", "unknownConceptUuid", "Unknown", "Unknown");
        parentConcept.addSetMember(unknownConcept);

        when(conceptService.getConceptByUuid("PulseDataUuid")).thenReturn(parentConcept);
        when(conceptService.getConceptNumeric(1)).thenReturn(valueConcept);
        AdditionalBahmniObservationFields additionalBahmniObservationFields = new AdditionalBahmniObservationFields(encounterUuid, new Date(), new Date(), obsGroupUuid);
        BahmniObservation bahmniObservation = etObsToBahmniObsMapper.map(parentObservation, additionalBahmniObservationFields, asList(parentConcept), true);

        assertEquals("Unknown", bahmniObservation.getValueAsString());
        assertEquals(true, bahmniObservation.isUnknown());
        assertTrue(bahmniObservation.getHiNormal().equals(100.0));
        assertTrue(bahmniObservation.getLowNormal().equals(50.0));
    }
}
