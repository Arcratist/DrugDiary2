package brettdansmith.drugdiary.ui.profile;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocationAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private final Context context;
    private List<String> resultList = new ArrayList<>();
    private final Geocoder geocoder;

    public LocationAutoCompleteAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.context = context;
        this.geocoder = new Geocoder(context);
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return resultList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }
        TextView text = convertView.findViewById(android.R.id.text1);
        text.setText(getItem(position));
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() > 2) {
                    List<String> locations = new ArrayList<>();
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(constraint.toString(), 5);
                        if (addresses != null) {
                            for (Address address : addresses) {
                                StringBuilder sb = new StringBuilder();
                                if (address.getLocality() != null) sb.append(address.getLocality());
                                if (address.getAdminArea() != null) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(address.getAdminArea());
                                }
                                if (address.getCountryName() != null) {
                                    if (sb.length() > 0) sb.append(", ");
                                    sb.append(address.getCountryName());
                                }
                                String formatted = sb.toString();
                                if (!formatted.isEmpty() && !locations.contains(formatted)) {
                                    locations.add(formatted);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore geocoder errors (e.g., no network)
                    }
                    filterResults.values = locations;
                    filterResults.count = locations.size();
                }
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    resultList = (List<String>) results.values;
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}
