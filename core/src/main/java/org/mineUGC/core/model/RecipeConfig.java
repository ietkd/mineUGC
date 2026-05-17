package org.mineUGC.core.model;

import java.util.List;
import java.util.Map;

public class RecipeConfig {
    private List<String> shape;
    private Map<String, String> ingredients;

    public List<String> getShape() { return shape; }
    public void setShape(List<String> shape) { this.shape = shape; }
    public Map<String, String> getIngredients() { return ingredients; }
    public void setIngredients(Map<String, String> ingredients) { this.ingredients = ingredients; }

    public boolean isValid() {
        return shape != null && !shape.isEmpty() && ingredients != null && !ingredients.isEmpty();
    }
}
