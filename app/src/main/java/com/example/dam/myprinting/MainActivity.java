package com.example.dam.myprinting;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FloatingActionButton fab;

    private Button btImprimir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.fab = (FloatingActionButton) findViewById(R.id.fab);
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);

        btImprimir = findViewById(R.id.btImprimir);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        btImprimir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                doPrint();

            }
        });
    }

    public void doPrint(){

        //Cogemos el servicio con el objeto printManager.

        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        String jobName = this.getString(R.string.app_name) + " Document";

        //Print nos permite hacer el trabajo de impresión.

        printManager.print(jobName, new MyPrintDocumentAdapter(this), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MyPrintDocumentAdapter extends PrintDocumentAdapter {

        Context context;
        private int pageHeight;
        private int pageWidth;
        public PdfDocument myPdfDocument;
        public int totalpages = 4;

        //También tenemos onStart() y onFinish():  son opcionales.

        public MyPrintDocumentAdapter(Context context) {

            this.context = context;

        }

        @Override
        public void onLayout(PrintAttributes oldAttributes,
                             PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback, //retrollamada
                             Bundle metadata) {

            //PrintePDFDocument, subclase especializada en imprimir pdfs.
            //newAttributes:
            myPdfDocument = new PrintedPdfDocument(context, newAttributes);

            //Esto nos viene del tamaño seleccionado por el usuario a la hora de imprimir.
            //De este modo podemos coger el resto de atributos.
            //Esto no es obligatorio.
            pageHeight = newAttributes.getMediaSize().getHeightMils()/1000 * 72;
            pageWidth = newAttributes.getMediaSize().getWidthMils()/1000 * 72;

            if (cancellationSignal.isCanceled()) {

                callback.onLayoutCancelled(); //Si el usuario cancela la impresión.

                return;

            }

            //Importante (si el layout no controla esto)

            if (totalpages > 0) {

                /* Creamos un documento info, siempre será así. Este llevará los metadatos del archivo. */

                PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                        .Builder("print_output.pdf") //Nombre del documento con el que hemos decidio guardarlo
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT) //Se trata de un documento
                        .setPageCount(totalpages); //Numpáginas

                PrintDocumentInfo info = builder.build();

                callback.onLayoutFinished(info, true); //Metadatos + true (ha habido cambios)

            } else {

                callback.onLayoutFailed("Page count is zero.");

            }
        }

        @Override
        public void onWrite(final PageRange[] pageRanges,
                            final ParcelFileDescriptor destination,
                            final CancellationSignal cancellationSignal,
                            final WriteResultCallback callback) {

            for (int i = 0; i < totalpages; i++) {

                if (pageInRange(pageRanges, i)){ //Controlar el rango de páginas

                    PdfDocument.PageInfo newPage = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create(); //Esto no es obligatorio, se puede hacer de otro modo
                                                                                                                        //Ver web de android

                    PdfDocument.Page page = myPdfDocument.startPage(newPage);

                    if (cancellationSignal.isCanceled()) {

                        callback.onWriteCancelled();
                        myPdfDocument.close();
                        myPdfDocument = null;

                        return;
                    }

                    drawPage(page, i);
                    myPdfDocument.finishPage(page);
                }
            }

            try {

                myPdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));

            } catch (IOException e) {

                callback.onWriteFailed(e.toString());
                return;

            } finally {

                myPdfDocument.close();
                myPdfDocument = null;

            }

            callback.onWriteFinished(pageRanges);
        }

        private boolean pageInRange(PageRange[] pageRanges, int page){

            for (int i = 0; i<pageRanges.length; i++) {

                if ((page >= pageRanges[i].getStart()) && (page <= pageRanges[i].getEnd()))

                    return true;
            }

            return false;
        }

        //Metodo utilizado normalmente para pintar cosas en java/android
        private void drawPage(PdfDocument.Page page, int pagenumber) {

            Canvas canvas = page.getCanvas(); //Un objeto canvas para cada página

            /*Canvas tiene muchos métodos que nos permiten pintar*/

            pagenumber++; // Make sure page numbers start at 1

            //Origen: esquina superior izquieda.
            int titleBaseLine = 72;
            int leftMargin = 54;

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(40);

            canvas.drawText(
                    "Test Print Document Page " + pagenumber,
                    leftMargin,
                    titleBaseLine,
                    paint);

            paint.setTextSize(14);

            canvas.drawText("This is some test content to verify that custom document printing works", leftMargin, titleBaseLine + 35, paint);

            if (pagenumber % 2 == 0)
                paint.setColor(Color.RED);
            else
                paint.setColor(Color.GREEN);

            PdfDocument.PageInfo pageInfo = page.getInfo();

            canvas.drawCircle(pageInfo.getPageWidth()/2,
                    pageInfo.getPageHeight()/2,
                    150,
                    paint);
        }
    }
}
