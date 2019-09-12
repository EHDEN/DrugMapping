SELECT ingredient.concept_id,
       atc.concept_code AS atc
FROM @vocab.concept_relationship relation
  LEFT OUTER JOIN @vocab.concept atc
    ON relation.concept_id_1 = atc.concept_id
  LEFT OUTER JOIN @vocab.concept ingredient
    ON relation.concept_id_2 = ingredient.concept_id
WHERE relation.relationship_id = 'ATC - RxNorm'
AND   ingredient.domain_id = 'Drug'
AND   ingredient.vocabulary_id LIKE 'RxNorm%'
AND   ingredient.concept_class_id = 'Ingredient'
AND   ingredient.standard_concept = 'S'
AND   ingredient.invalid_reason IS NULL