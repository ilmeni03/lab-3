package server.protocol;

/**
 * Rappresenta una risposta JSON dal server.
 * Contiene sempre: success, message, e dati opzionali.
 */
public class JsonResponse {
    private boolean success;
    private String message;
    private Object data;  // Dati specifici della risposta
    
    public JsonResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }
    
    public JsonResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    // Factory methods per risposte comuni
    
    public static JsonResponse success(String message) {
        return new JsonResponse(true, message);
    }
    
    public static JsonResponse success(String message, Object data) {
        return new JsonResponse(true, message, data);
    }
    
    public static JsonResponse error(String message) {
        return new JsonResponse(false, message);
    }
    
    // Getters e Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}
