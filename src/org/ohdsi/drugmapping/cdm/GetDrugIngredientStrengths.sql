SELECT drug.concept_id AS drug_concept_id,
       drug.concept_name AS drug_concept_name,
       drug.domain_id AS drug_domain_id,
       drug.vocabulary_id AS drug_vocabulary_id,
       drug.concept_class_id AS drug_concept_class_id,
       drug.standard_concept AS drug_standard_concept,
       drug.concept_code AS drug_concept_code,
       drug.invalid_reason AS drug_invalid_reason,
       ingredient_concept_id,
       amount_value,
       amount_unit_concept_id,
       numerator_value,
       numerator_unit_concept_id,
       denominator_value,
       denominator_unit_concept_id,
       form.concept_id AS form_concept_id
FROM @vocab.drug_strength
  INNER JOIN @vocab.concept drug
    ON drug_strength.drug_concept_id = drug.concept_id
  LEFT JOIN @vocab.concept_relationship
    ON concept_relationship.concept_id_1 = drug.concept_id
  LEFT JOIN @vocab.concept form
    ON form.concept_id = concept_relationship.concept_id_2
WHERE concept_relationship.relationship_id = 'RxNorm has dose form'
AND   drug_strength.invalid_reason IS NULL
AND   drug.concept_class_id = 'Clinical Drug'
AND   drug.standard_concept = 'S'
AND   drug.invalid_reason IS NULL
GROUP BY drug.concept_id,
         ingredient_concept_id,
         amount_value,
         amount_unit_concept_id,
         numerator_value,
         numerator_unit_concept_id,
         denominator_value,
         denominator_unit_concept_id,
         form.concept_id