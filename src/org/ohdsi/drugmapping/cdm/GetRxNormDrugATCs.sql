SELECT DISTINCT strength.drug_concept_id as concept_id,
                atc.concept_code AS atc
FROM @vocab.drug_strength strength
INNER JOIN @vocab.concept_ancestor ancestor
    ON (ancestor.descendant_concept_id = strength.drug_concept_id)
INNER JOIN @vocab.concept atc
    ON (atc.concept_id = ancestor.ancestor_concept_id) AND (atc.vocabulary_id = 'ATC')