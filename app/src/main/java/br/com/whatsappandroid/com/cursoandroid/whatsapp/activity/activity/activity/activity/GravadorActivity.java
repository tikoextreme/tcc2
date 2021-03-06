package br.com.whatsappandroid.com.cursoandroid.whatsapp.activity.activity.activity.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import br.com.whatsappandroid.com.cursoandroid.whatsapp.R;
import br.com.whatsappandroid.com.cursoandroid.whatsapp.activity.activity.activity.activity.helper.VisualizerView;
import br.com.whatsappandroid.com.cursoandroid.whatsapp.activity.activity.activity.activity.model.Gravacao;
import br.com.whatsappandroid.com.cursoandroid.whatsapp.activity.activity.activity.activity.model.Paciente;
import io.realm.Realm;

import static android.content.ContentValues.TAG;

public class GravadorActivity extends Activity {

    // Variáveis da interface gráfica
    private VisualizerView visualizer;
    private ToggleButton recordButton;
    private Button saveButton;
    private Button deleteButton;
    private Button viewSavedRecordingsButton;

    private static final String TAG = GravadorActivity.class.getName();
    private MediaRecorder recorder;
    private Handler handler;
    private boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gravador);

        recordButton = (ToggleButton) findViewById(R.id.recordButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setEnabled(false);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setEnabled(false);
        viewSavedRecordingsButton = (Button) findViewById(R.id.viewSavedRecordingsButton);
        visualizer = (VisualizerView) findViewById(R.id.visualizerView);

        handler = new Handler();


        recordButton.setOnCheckedChangeListener(recordButtonListener);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.name_edittext, null);
                final EditText nameEditText = (EditText) view.findViewById(R.id.nameEditText);

                AlertDialog.Builder inputDialog = new AlertDialog.Builder(GravadorActivity.this);
                inputDialog.setView(view);
                inputDialog.setTitle("Gravação");
                inputDialog.setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameEditText.getText().toString().trim();

                       if (name.length() != 0){
                            File tempFile = (File) v.getTag();
                            File newFile = new File(getExternalFilesDir(null).getAbsolutePath() + File.separator + name + ".3gp");
                            tempFile.renameTo(newFile);

                           Realm realm = Realm.getDefaultInstance();// ... Utilização da base...realm.close();
                           realm.beginTransaction();

                           Gravacao gravacao = new Gravacao();
                           gravacao.setIdGravacao(1);
                           gravacao.setNome(name); // nome da gravação informada pelo usuario
                           gravacao.setDataGravacao(new Date());
                           //gravacao.setArquivoAudio(newFile);

                           realm.copyToRealm(gravacao);

                           Toast.makeText(GravadorActivity.this, "Gravação:  " + gravacao.getNome() + " cadastrada com sucesso!", Toast.LENGTH_LONG).show();

                           realm.commitTransaction();
                           realm.close();



                            saveButton.setEnabled(false);
                            deleteButton.setEnabled(false);
                            recordButton.setEnabled(true);
                            viewSavedRecordingsButton.setEnabled(true);
                        }
                        else {
                            Toast message = Toast.makeText(GravadorActivity.this, "ERRO PESSOAL", Toast.LENGTH_SHORT);
                            message.setGravity(Gravity.CENTER, message.getXOffset()/2, message.getYOffset()/2);
                            message.show();
                        }
                    }
                });

                inputDialog.setNegativeButton("Cancelar", null);
                inputDialog.show();
             //   inputDialog.setPositiveButton()
            }
        });


        viewSavedRecordingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GravadorActivity.this, SavedRecordings.class);
                startActivity( intent );

            }
        });

    }
    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (recording){
                int x = recorder.getMaxAmplitude(); // Aqui pega o maximo valor da amostragem
                visualizer.addAmplitude(x);
                visualizer.invalidate();
                handler.postDelayed(this, 50);
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        // registra o receptor de recordButton
        recordButton.setOnCheckedChangeListener(recordButtonListener);
    } // fim do método onResume

    @Override
    protected void onPause(){
        super.onPause();
        recordButton.setOnCheckedChangeListener(null);

        if (recorder != null){
            handler.removeCallbacks(updateVisualizer);
            visualizer.clear();
            recordButton.setChecked(false);
            viewSavedRecordingsButton.setEnabled(true);
            recorder.release();
            recording = false;
            recorder = null;
            ((File) deleteButton.getTag()).delete(); // exclui o arquivo temporário
        }
    }

    CompoundButton.OnCheckedChangeListener recordButtonListener = new CompoundButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked){
                visualizer.clear();

                saveButton.setEnabled(false);
                deleteButton.setEnabled(false);
                viewSavedRecordingsButton.setEnabled(false);

                if (recorder == null)
                    recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioEncodingBitRate(16);
                recorder.setAudioSamplingRate(44100);

                try{
                    File tempFile = File.createTempFile("VoiceRecorder", ".3gp", getExternalFilesDir(null));
                    saveButton.setTag(tempFile);
                    deleteButton.setTag(tempFile);
                    recorder.setOutputFile(tempFile.getAbsolutePath());
                    recorder.prepare();
                    recorder.start();
                    recording = true;
                    handler.post(updateVisualizer);
                } // fim do try
                catch (IllegalStateException e){
                    Log.e(TAG, e.toString());
                } catch (IOException e){
                    Log.e(TAG, e.toString());
                } // fim do catch
            } // fim do if
            else{
                    recorder.stop();
                    recorder.reset();
                    recording = false;
                    saveButton.setEnabled(true);
                    deleteButton.setEnabled(true);
                    recordButton.setEnabled(false);
            } // fim do else
        } // fim do método
    };


}
