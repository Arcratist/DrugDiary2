package brettdansmith.drugdiary.network.ai;

public interface AiStreamCallback {
    void onChunk(String text);
    void onDone();
    void onError(String error);
}

