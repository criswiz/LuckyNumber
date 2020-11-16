package com.example.luckynumber;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.luckynumber.Common.Common;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    TextView txt_score, txt_count, txt_result, txt_money, txt_status;
    EditText edt_bet_value, edt_money;
    Button btn_submit, btn_disconnect;

    Socket socket;

    Boolean isDisconnected=false, isBet=false, canPlay=true;

    int resutlNumber = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_score = findViewById(R.id.txt_score);
        txt_count = findViewById(R.id.txt_count);
        txt_result = findViewById(R.id.txt_result);
        txt_money = findViewById(R.id.txt_money);
        txt_status = findViewById(R.id.txt_status);

        edt_bet_value = findViewById(R.id.edt_bet_value);
        edt_money = findViewById(R.id.edt_money_bet_value);

        btn_submit = findViewById(R.id.btn_submit);
        btn_disconnect = findViewById(R.id.btn_disconnect);

        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDisconnected){
                    socket.disconnect();
                    btn_disconnect.setText(R.string.connect);
                }else {
                    socket.connect();
                    btn_disconnect.setText(R.string.disconnect);
                }

                isDisconnected = !isDisconnected;
            }
        });

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (canPlay){
                        if (!isBet){
                            int money_bet_value = Integer.parseInt(edt_money.getText().toString());
                            if (Common.score >= money_bet_value){
                                //JSON object to send
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("money", edt_money.getText().toString());
                                jsonObject.put("betValue", edt_money.getText().toString());

                                socket.emit("client_send_money", jsonObject);

                                Common.score -= money_bet_value;
                                txt_score.setText(String.valueOf(Common.score));

                                isBet = true; // Prevent multiple bet in 1 turn
                            }else {
                                Toast.makeText(MainActivity.this, "You don't have enough money, please restart game", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(MainActivity.this, "You have already placed a bet", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(MainActivity.this, "Please wait for next turn", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        //Connect to socket server
        try {
            socket = IO.socket("http://10.0.2.2:3000");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }
            });

            socket.connect();
            registerAllEventForGame();

        }catch (Exception ex){
            Toast.makeText(this, ""+ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void registerAllEventForGame() {
        socket.on("broadcast", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Retrieve timer
                        txt_count.setText(new StringBuilder("Timer: ").append(args[0]));
                        txt_result.setText("");
                        txt_status.setText("");
                    }
                });
            }
        });

        socket.on("wait_before_restart", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                canPlay = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_status.setText(new StringBuilder().append("Please wait for ").append(args[0]).append(" seconds").toString());
                        txt_count.setText(R.string.wait);
                        isBet = false;
                    }
                });
            }
        });

        socket.on("money_send", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_money.setText(String.valueOf(args[0]));
                    }
                });
            }
        });

        socket.on("restart", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_result.setVisibility(View.GONE);
                    }
                });
            }
        });

        socket.on("result", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {

                resutlNumber = Integer.parseInt(args[0].toString());

                 runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                        txt_result.setVisibility(View.VISIBLE);
                        txt_result.setText(new StringBuilder("Result: ").append(args[0].toString()));
                     }
                 });
            }
        });

        socket.on("reward", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //User win
                        txt_result.setText(new StringBuilder("Result: ").append(resutlNumber).append(" | Congratulations You Won!!! ")
                        .append(args[0])
                        .append(" USD"));

                        txt_result.setBackgroundResource(R.drawable.win_text_view_bg);
                        Log.d("REWARD", "You receive "+args[0]+ " USD");
                        Common.score += Integer.parseInt(args[0].toString());

                        txt_score.setText(String.valueOf(Common.score));
                    }
                });
            }
        });

        socket.on("lose", new Emitter.Listener() {
            @Override
            public void call(final Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int money = Integer.parseInt(args[0].toString());
                        txt_result.setText(new StringBuilder("Result: ")
                        .append(resutlNumber)
                        .append(" | ")
                        .append("You lose ")
                        .append(money)
                        .append(" USD"));
                        txt_result.setBackgroundResource(R.drawable.lose_text_view_bg);
                    }
                });
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_result.setText("DISCONNECT");
                        txt_score.setText("DISCONNECT");
                        txt_money.setText("DISCONNECT");
                    }
                });
            }
        });
    }
}
