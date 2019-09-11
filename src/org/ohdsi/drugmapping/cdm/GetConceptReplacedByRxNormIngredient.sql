SELECT replaced_concept.concept_name
,      ingredient.concept_id
FROM @vocab.concept_relationship replaced_by
LEFT OUTER JOIN @vocab.concept replaced_concept
  ON replaced_by.concept_id_1 = replaced_concept.concept_id
LEFT OUTER JOIN @vocab.concept ingredient
  ON replaced_by.concept_id_2 = ingredient.concept_id
WHERE relationship_id = 'Concept replaced by'
AND ingredient.domain_id = 'Drug'
AND ingredient.vocabulary_id LIKE 'RxNorm%'
AND ingredient.concept_class_id = 'Ingredient'
AND ingredient.standard_concept = 'S'
AND ingredient.invalid_reason IS NULL