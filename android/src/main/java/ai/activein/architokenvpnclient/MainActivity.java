package ai.activein.architokenvpnclient;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.rgb(15, 23, 42));

        TextView title = new TextView(this);
        title.setText("ArchIToken-VPN Client");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Android MVP: VLESS Reality link checker and profile preview.");
        subtitle.setTextColor(Color.rgb(203, 213, 225));
        subtitle.setTextSize(14);
        root.addView(subtitle);

        input = new EditText(this);
        input.setHint("Paste vless:// link");
        input.setSingleLine(false);
        input.setMinLines(4);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setBackgroundColor(Color.rgb(30, 41, 59));
        root.addView(input);

        Button parse = new Button(this);
        parse.setText("Parse");
        parse.setOnClickListener(v -> parseLink());
        root.addView(parse);

        output = new TextView(this);
        output.setTextColor(Color.rgb(226, 232, 240));
        output.setTextSize(14);
        output.setText("Waiting for VLESS Reality link.");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);
    }

    private void parseLink() {
        String link = input.getText().toString().trim();
        if (!link.startsWith("vless://")) {
            output.setText("Invalid input: link must start with vless://");
            return;
        }
        String body = link.substring("vless://".length());
        String code = "CUSTOM-NOD-A1";
        int fragment = body.indexOf('#');
        if (fragment >= 0 && fragment + 1 < body.length()) {
            code = body.substring(fragment + 1);
            body = body.substring(0, fragment);
        }
        int at = body.indexOf('@');
        int query = body.indexOf('?');
        String uuid = at > 0 ? body.substring(0, at) : "";
        String server = at > 0 ? body.substring(at + 1, query > at ? query : body.length()) : "";
        String params = query >= 0 ? body.substring(query + 1) : "";
        output.setText(
                "Node: " + code + "\n"
                        + "Server: " + server + "\n"
                        + "UUID: " + mask(uuid) + "\n"
                        + "Params: " + params.replace("&", "\n")
        );
    }

    private String mask(String value) {
        if (value.length() <= 10) return "{hidden}";
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }
}

