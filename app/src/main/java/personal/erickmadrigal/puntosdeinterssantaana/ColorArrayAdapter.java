package personal.erickmadrigal.puntosdeinterssantaana;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


/**
 * Created by erick on 2/27/2016.
 * From http://www.codexpedia.com/android/custom-android-arrayadaper-alternate-background-color-for-elements-in-arrayadapter/
 */
public class ColorArrayAdapter extends ArrayAdapter<Object> {
    private String[] list;
    private int[] colors;
    private int size_colors;

    public ColorArrayAdapter(Context context, int textViewResourceId,
                             Object[] objects, int[] colors) {
        super(context, textViewResourceId, objects);
        list = new String[objects.length];
        final int list_length = list.length;// Moved  list.length call out of the loop to local variable list_length
        for (int i = 0; i < list_length; i++) {
            list[i] = (String) objects[i];
        }
        this.colors = colors;
        size_colors = colors.length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View view = (View)super.getView(position, convertView, parent);
        TextView text = (TextView) view.findViewById(android.R.id.text1);
        text.setTextColor(colors[position % size_colors]);
        return view;
    }

}
