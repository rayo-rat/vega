package com.example.exam_p3;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class BancoAdapter extends ArrayAdapter<BancoItem> {

    public BancoAdapter(Context ctx, ArrayList<BancoItem> data) {
        super(ctx, 0, data);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return crearVista(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return crearVista(position, convertView, parent);
    }

    private View crearVista(int pos, View convert, ViewGroup parent) {
        if (convert == null) {
            convert = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_banco, parent, false);
        }

        BancoItem item = getItem(pos);

        ImageView icon = convert.findViewById(R.id.imgBanco);
        TextView texto = convert.findViewById(R.id.txtBanco);

        icon.setImageResource(item.icono);
        texto.setText(item.nombre);

        return convert;
    }
}
