SELECT relationship.relationship_id,
       drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       drug.vocabulary_id AS drug_vocabulary_id,
       drug.concept_class_id AS drug_concept_class_id,
       drug.concept_code AS drug_concept_code,
       ingredient.concept_id AS ingredient_concept_id,
       synonym.concept_synonym_name AS drug_synonym_name
FROM @vocab.concept_relationship relationship
  LEFT OUTER JOIN @vocab.concept drug
    ON relationship.concept_id_1 = drug.concept_id
  LEFT OUTER JOIN @vocab.concept ingredient
    ON relationship.concept_id_2 = ingredient.concept_id
  LEFT OUTER JOIN @vocab.concept_synonym synonym
    ON drug.concept_id = synonym.concept_id
WHERE ingredient.domain_id = 'Drug'
AND   ingredient.vocabulary_id LIKE 'RxNorm%'
AND   ingredient.concept_class_id = 'Ingredient'
AND   ingredient.standard_concept = 'S'
AND   ingredient.invalid_reason IS NULL
ORDER BY relationship.relationship_id,
         drug.concept_id,
         synonym.concept_synonym_name,
         ingredient.concept_id