package brettdansmith.drugdiary.ui.assistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.R;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<JSONObject> sessions = new ArrayList<>();
    private String activeSessionId = "";
    private OnSessionClickListener listener;

    public interface OnSessionClickListener {
        void onSessionClick(String sessionId);
        void onSessionDelete(String sessionId);
    }

    public SessionAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void submitSessions(JSONArray sessionArray, String activeId) {
        this.sessions.clear();
        if (sessionArray != null) {
            for (int i = 0; i < sessionArray.length(); i++) {
                JSONObject s = sessionArray.optJSONObject(i);
                if (s != null) this.sessions.add(s);
            }
        }
        this.activeSessionId = activeId == null ? "" : activeId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assistant_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        JSONObject session = sessions.get(position);
        String id = session.optString("id");
        String title = session.optString("title", "New Chat");
        
        holder.textTitle.setText(title);
        
        boolean isActive = id.equals(activeSessionId);
        holder.itemView.setSelected(isActive);
        holder.itemView.setAlpha(isActive ? 1.0f : 0.8f);
        holder.buttonDelete.setVisibility(View.VISIBLE);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(id);
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) listener.onSessionDelete(id);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        View buttonDelete;

        SessionViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_session_title);
            buttonDelete = itemView.findViewById(R.id.button_delete_session);
        }
    }
}
