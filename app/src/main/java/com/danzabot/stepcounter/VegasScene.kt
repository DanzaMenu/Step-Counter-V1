package com.danzabot.stepcounter
import android.content.Context
import android.graphics.*
import kotlin.math.*
object VegasScene {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val path=Path()
    private val startNanos=System.nanoTime()
    private var portraitSource:Bitmap?=null
    private var widgetSource:Bitmap?=null
    fun draw(context:Context,canvas:Canvas,ox:Float,oy:Float,w:Float,h:Float,u:Float,weatherKind:String,isNight:Boolean,alpha:Int=255){
        val t=(System.nanoTime()-startNanos)/1_000_000_000f
        val a=alpha.coerceIn(0,255)
        drawApprovedArtwork(context,canvas,ox,oy,w,h,a);drawAmbientPulse(canvas,ox,oy,w,h,t,a);drawWeather(canvas,ox,oy,w,h,u,t,weatherKind,a)
    }
    private fun drawApprovedArtwork(context:Context,c:Canvas,x:Float,y:Float,w:Float,h:Float,a:Int){
        val portrait=w/h<1.25f
        var bitmap=if(portrait)portraitSource else widgetSource
        if(bitmap==null||bitmap.isRecycled){bitmap=BitmapFactory.decodeResource(context.resources,if(portrait)R.drawable.vegas_strip_background else R.drawable.vegas_strip_widget);if(portrait)portraitSource=bitmap else widgetSource=bitmap}
        val b=bitmap?:return
        val src=if(portrait){val target=w/h;val sw=(b.height*target).roundToInt().coerceAtMost(b.width);val left=(b.width-sw)/2;Rect(left,0,left+sw,b.height)}else{val displayedRatio=2.9f;val sh=(b.width/displayedRatio).roundToInt().coerceAtMost(b.height);val top=((b.height-sh)*.44f).roundToInt().coerceIn(0,b.height-sh);Rect(0,top,b.width,top+sh)}
        p.alpha=a;p.isFilterBitmap=true;c.drawBitmap(b,src,RectF(x,y,x+w,y+h),p);p.alpha=255
    }
    private fun drawAmbientPulse(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val pulse=(10+8*(.5f+.5f*sin(t*.8f))).roundToInt();p.shader=LinearGradient(x,y+h*.45f,x+w,y+h,intArrayOf(Color.TRANSPARENT,Color.argb(pulse*a/255,255,34,191),Color.argb(pulse*a/255,32,219,255)),null,Shader.TileMode.CLAMP);c.drawRect(x,y,x+w,y+h,p);p.shader=null
    }
    private fun drawSky(c:Canvas,x:Float,y:Float,w:Float,h:Float,kind:String,night:Boolean,a:Int){
        val top=when(kind){"storm"->0xFF080817.toInt();"rain"->0xFF10142B.toInt();"fog"->0xFF30334E.toInt();else->if(night)0xFF080B35.toInt() else 0xFF182B68.toInt()};val bottom=when(kind){"storm"->0xFF241336.toInt();"rain"->0xFF29204B.toInt();"fog"->0xFF655579.toInt();else->0xFF5A197C.toInt()}
        p.shader=LinearGradient(x,y,x,y+h,top,bottom,Shader.TileMode.CLAMP);p.alpha=a;c.drawRect(x,y,x+w,y+h,p);p.shader=null;p.alpha=255;p.shader=RadialGradient(x+w*.5f,y+h*.45f,w*.65f,intArrayOf(Color.argb(a/2,244,45,188),Color.TRANSPARENT),floatArrayOf(0f,1f),Shader.TileMode.CLAMP);c.drawRect(x,y,x+w,y+h,p);p.shader=null
    }
    private fun drawStars(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,kind:String,a:Int){
        if(kind=="cloud"||kind=="rain"||kind=="storm"||kind=="fog")return
        for(i in 0 until 32){val sx=x+w*Fx.rnd(811L+i*71L);val sy=y+h*.34f*Fx.rnd(419L+i*97L);val tw=.45f+.55f*abs(sin(t*.8f+i));p.color=Color.argb((a*tw).toInt(),if(i%4==0)255 else 190,if(i%3==0)176 else 220,255);c.drawCircle(sx,sy,max(1f,w/1200f)*(if(i%7==0)2f else 1f),p)}
    }
    private fun drawMountains(c:Canvas,x:Float,y:Float,w:Float,h:Float,a:Int){
        p.color=Color.argb(a,30,18,72);path.reset();path.moveTo(x,y+h*.43f);for(i in 0..16){path.lineTo(x+w*i/16f,y+h*(.26f+.13f*Fx.rnd(900L+i*61L)))};path.lineTo(x+w,y+h*.58f);path.lineTo(x,y+h*.58f);path.close();c.drawPath(path,p)
        p.color=Color.argb(a,56,24,94);path.reset();path.moveTo(x,y+h*.47f);for(i in 0..12){path.lineTo(x+w*i/12f,y+h*(.34f+.10f*Fx.rnd(1700L+i*83L)))};path.lineTo(x+w,y+h*.59f);path.lineTo(x,y+h*.59f);path.close();c.drawPath(path,p)
    }
    private fun drawSearchlights(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val colors=intArrayOf(0xFF17CFFF.toInt(),0xFFE72EFF.toInt(),0xFF6D5CFF.toInt(),0xFFFF38D1.toInt());for(i in 0..3){val bx=x+w*(.12f+i*.25f);val sway=sin(t*.18f+i*1.7f)*w*.10f;path.reset();path.moveTo(bx-w*.008f,y+h*.62f);path.lineTo(bx+sway-w*.025f,y);path.lineTo(bx+sway+w*.025f,y);path.lineTo(bx+w*.008f,y+h*.62f);path.close();val col=colors[i];p.shader=LinearGradient(bx,y+h*.62f,bx+sway,y,Color.argb(a/2,Color.red(col),Color.green(col),Color.blue(col)),Color.TRANSPARENT,Shader.TileMode.CLAMP);c.drawPath(path,p);p.shader=null}
    }
    private fun drawResorts(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        drawPyramid(c,x+w*.02f,y+h*.12f,w*.22f,h*.49f,t,a);drawCastle(c,x,y+h*.43f,w*.23f,h*.34f,t,a);drawParis(c,x+w*.27f,y+h*.09f,w*.18f,h*.58f,t,a);drawCenterHotels(c,x+w*.39f,y+h*.31f,w*.25f,h*.39f,a);drawWheel(c,x+w*.72f,y+h*.06f,w*.22f,h*.54f,t,a);drawFountainHotel(c,x+w*.62f,y+h*.34f,w*.38f,h*.39f,t,a)
    }
    private fun drawPyramid(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        p.shader=LinearGradient(x,y,x+w,y+h,0xFF18438F.toInt(),0xFF130B36.toInt(),Shader.TileMode.CLAMP);path.reset();path.moveTo(x+w*.5f,y);path.lineTo(x,y+h);path.lineTo(x+w,y+h);path.close();c.drawPath(path,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=max(2f,w*.012f);p.color=Color.argb(a,47,231,255);c.drawPath(path,p);p.strokeWidth=max(1f,w*.006f);p.color=Color.argb(a,255,47,225);c.drawLine(x+w*.5f,y,x+w,y+h,p);for(i in 1..7){val yy=y+h*i/8f;val f=i/8f;c.drawLine(x+w*.5f-w*.5f*f,yy,x+w*.5f+w*.5f*f,yy,p)};p.style=Paint.Style.FILL
        p.shader=LinearGradient(x+w*.46f,y,x+w*.54f,y,Color.TRANSPARENT,Color.argb(a,92,243,255),Shader.TileMode.MIRROR);path.reset();path.moveTo(x+w*.47f,y);path.lineTo(x+w*.30f,y-h*.75f);path.lineTo(x+w*.70f,y-h*.75f);path.lineTo(x+w*.53f,y);path.close();c.drawPath(path,p);p.shader=null;drawMarquee(c,x+w*.06f,y+h*.68f,w*.35f,h*.20f,"DESERT",0xFFFF8B28.toInt(),t,a)
    }
    private fun drawCastle(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val towers=floatArrayOf(.04f,.22f,.43f,.65f,.84f);for(i in towers.indices){val cx=x+w*towers[i];val tw=w*(if(i==2).16f else .12f);val top=y+h*(if(i%2==0).06f else .18f);p.color=Color.argb(a,246,221,205);c.drawRoundRect(cx,top,cx+tw,y+h*.82f,tw*.12f,tw*.12f,p);p.color=if(i%2==0)0xFF20BFFF.toInt() else 0xFFFF335B.toInt();path.reset();path.moveTo(cx-tw*.12f,top);path.lineTo(cx+tw*.5f,top-h*.30f);path.lineTo(cx+tw*1.12f,top);path.close();c.drawPath(path,p);p.color=Color.argb(a,104,51,156);for(r in 0..2)c.drawCircle(cx+tw*.5f,top+h*(.16f+r*.17f),tw*.11f,p)};p.color=Color.argb(a,231,199,188);c.drawRect(x,y+h*.58f,x+w,y+h,p);drawMarquee(c,x+w*.22f,y+h*.55f,w*.55f,h*.32f,"ROYAL CASTLE",0xFFFF38D1.toInt(),t,a)
    }
    private fun drawParis(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val cx=x+w*.48f;val top=y;val base=y+h;p.color=Color.argb(a,255,183,54);p.style=Paint.Style.STROKE;p.strokeWidth=max(2f,w*.035f);path.reset();path.moveTo(cx,top);path.lineTo(x+w*.18f,base);path.moveTo(cx,top);path.lineTo(x+w*.78f,base);path.moveTo(x+w*.27f,y+h*.72f);path.lineTo(x+w*.70f,y+h*.72f);path.moveTo(x+w*.36f,y+h*.43f);path.lineTo(x+w*.61f,y+h*.43f);c.drawPath(path,p);p.strokeWidth=max(1f,w*.012f);for(i in 1..7){val yy=y+h*i/8f;val half=w*(.03f+.31f*i/8f);c.drawLine(cx-half,yy,cx+half,yy,p)};p.style=Paint.Style.FILL;p.color=Color.argb(a,255,222,84);c.drawCircle(cx,top,w*.045f,p);p.color=Color.RED;c.drawCircle(cx,top-w*.045f,w*.015f,p);drawMarquee(c,x,y+h*.70f,w*.52f,h*.22f,"RIVIERA",0xFF7C35FF.toInt(),t,a)
        val bx=x+w*.62f;val by=y+h*.58f;val br=w*.18f;p.shader=RadialGradient(bx,by,br,0xFF5A78FF.toInt(),0xFF12045B.toInt(),Shader.TileMode.CLAMP);c.drawCircle(bx,by,br,p);p.shader=null;p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,w*.012f);p.color=Color.argb(a,106,246,255);for(i in 0..7){val yy=by-br+2*br*i/7f;val dx=sqrt(max(0f,br*br-(yy-by)*(yy-by)));c.drawLine(bx-dx,yy,bx+dx,yy,p)};p.style=Paint.Style.FILL
    }
    private fun drawCenterHotels(c:Canvas,x:Float,y:Float,w:Float,h:Float,a:Int){
        p.shader=LinearGradient(x,y,x+w,y,0xFFFFB847.toInt(),0xFF7B2ED5.toInt(),Shader.TileMode.CLAMP);c.drawRoundRect(x,y,x+w,y+h,w*.025f,w*.025f,p);p.shader=null;p.color=Color.argb(a,255,218,115);for(col in 0..10)for(row in 0..6)if((col+row)%4!=0)c.drawRoundRect(x+w*(.04f+col*.085f),y+h*(.09f+row*.105f),x+w*(.065f+col*.085f),y+h*(.15f+row*.105f),2f,2f,p)
        p.color=Color.argb(a,29,11,72);c.drawRect(x+w*.64f,y-h*.18f,x+w*.94f,y+h*.12f,p);p.color=Color.argb(a,55,232,255);c.drawRect(x+w*.66f,y-h*.15f,x+w*.69f,y+h*.08f,p);c.drawRect(x+w*.89f,y-h*.15f,x+w*.92f,y+h*.08f,p);p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=max(7f,w*.07f);p.color=Color.argb(a,88,241,255);c.drawText("NEON",x+w*.79f,y-h*.04f,p);p.textAlign=Paint.Align.LEFT
    }
    private fun drawWheel(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val cx=x+w*.5f;val cy=y+h*.46f;val r=min(w*.47f,h*.44f);p.style=Paint.Style.STROKE;p.strokeWidth=max(2f,w*.015f);p.color=Color.argb(a,255,48,221);c.drawCircle(cx,cy,r,p);p.strokeWidth=max(1f,w*.008f);p.color=Color.argb(a,56,232,255);val rot=t*.06f;for(i in 0 until 16){val ang=rot+i*PI.toFloat()/8f;val ex=cx+cos(ang)*r;val ey=cy+sin(ang)*r;c.drawLine(cx,cy,ex,ey,p);p.style=Paint.Style.FILL;c.drawCircle(ex,ey,max(2f,w*.025f),p);p.style=Paint.Style.STROKE};p.style=Paint.Style.FILL;p.color=Color.argb(a,255,188,59);c.drawCircle(cx,cy,w*.04f,p)
    }
    private fun drawFountainHotel(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        p.shader=LinearGradient(x,y,x,y+h,0xFFFFD77D.toInt(),0xFF8B387D.toInt(),Shader.TileMode.CLAMP);c.drawRoundRect(x,y,x+w,y+h,w*.025f,w*.025f,p);p.shader=null;p.color=Color.argb(a,255,239,181);for(i in 0..12){val xx=x+w*(.035f+i*.075f);c.drawRect(xx,y+h*.15f,xx+w*.025f,y+h*.80f,p)};p.color=Color.argb(a,24,13,57);for(i in 0..11)for(j in 0..3)c.drawRect(x+w*(.055f+i*.078f),y+h*(.23f+j*.14f),x+w*(.072f+i*.078f),y+h*(.30f+j*.14f),p)
        drawMarquee(c,x+w*.15f,y+h*.46f,w*.62f,h*.25f,"BELLA VISTA",0xFFFFA51F.toInt(),t,a);val waterY=y+h*.92f;p.color=Color.argb(a,14,111,189);c.drawRect(x,waterY,x+w,y+h,p);for(i in 0..13){val jet=h*(.05f+.13f*(.5f+.5f*sin(t*1.5f+i*.8f)));val xx=x+w*(.03f+i*.072f);p.color=Color.argb(a,92,239,255);p.strokeWidth=max(1f,w*.008f);c.drawLine(xx,waterY,xx,waterY-jet,p)}
    }
    private fun drawMarquee(c:Canvas,x:Float,y:Float,w:Float,h:Float,text:String,color:Int,t:Float,a:Int){
        p.color=Color.argb((a*.85f).toInt(),24,7,55);c.drawRoundRect(x,y,x+w,y+h,h*.22f,h*.22f,p);p.style=Paint.Style.STROKE;p.strokeWidth=max(2f,w*.018f);p.color=Color.argb(a,Color.red(color),Color.green(color),Color.blue(color));c.drawRoundRect(x,y,x+w,y+h,h*.22f,h*.22f,p);p.style=Paint.Style.FILL;for(i in 0 until 14){val on=((i+(t*5).toInt())%3)==0;p.color=Color.argb(if(on)a else a/3,255,230,107);val bx=x+w*(i+.5f)/14;c.drawCircle(bx,y,max(1f,w*.012f),p);c.drawCircle(bx,y+h,max(1f,w*.012f),p)};p.textAlign=Paint.Align.CENTER;p.typeface=Typeface.DEFAULT_BOLD;p.textSize=min(h*.45f,w/(text.length*.68f));p.color=Color.WHITE;c.drawText(text,x+w*.5f,y+h*.64f,p);p.textAlign=Paint.Align.LEFT
    }
    private fun drawBoulevard(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,a:Int){
        val vy=y+h*.57f;path.reset();path.moveTo(x+w*.43f,vy);path.lineTo(x+w*.18f,y+h);path.lineTo(x+w*.82f,y+h);path.lineTo(x+w*.57f,vy);path.close();p.shader=LinearGradient(x,vy,x,y+h,0xFF30224A.toInt(),0xFF151427.toInt(),Shader.TileMode.CLAMP);c.drawPath(path,p);p.shader=null;p.color=Color.argb(a,255,222,122);p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,w*.0025f);c.drawLine(x+w*.43f,vy,x+w*.18f,y+h,p);c.drawLine(x+w*.57f,vy,x+w*.82f,y+h,p);p.color=Color.argb(a,248,222,255);for(side in -1..1 step 2)for(i in 0..6){val z=(i/7f+t*.12f)%1f;val yy=vy+h*.43f*z*z;val cx=x+w*.5f+side*w*(.025f+.17f*z);c.drawLine(cx,yy,cx+side*w*.012f*z,yy+h*(.012f+.045f*z),p)};p.style=Paint.Style.FILL;for(i in 0 until 13)drawCar(c,x,y,w,h,t,i,a)
    }
    private fun drawCar(c:Canvas,x:Float,y:Float,w:Float,h:Float,t:Float,i:Int,a:Int){
        val speed=.035f+Fx.rnd(7300L+i*41L)*.045f;val z=(Fx.rnd(5100L+i*73L)+t*speed)%1f;val lane=(i%4)-1.5f;val yy=y+h*(.59f+.40f*z*z);val cx=x+w*(.5f+lane*(.025f+.07f*z));val cw=w*(.008f+.030f*z);val ch=h*(.015f+.045f*z);p.color=if(i%5==0)Color.argb(a,255,186,27) else Color.argb(a,45+(i*31)%150,42+(i*53)%130,85+(i*47)%150);c.drawRoundRect(cx-cw,yy-ch,cx+cw,yy+ch,ch*.25f,ch*.25f,p);p.color=Color.argb(a,190,220,255);c.drawRect(cx-cw*.55f,yy-ch*.65f,cx+cw*.55f,yy-ch*.10f,p);p.color=if(i%2==0)Color.argb(a,255,245,205) else Color.argb(a,255,35,73);c.drawCircle(cx-cw*.62f,yy+ch*.50f,max(1f,cw*.18f),p);c.drawCircle(cx+cw*.62f,yy+ch*.50f,max(1f,cw*.18f),p)
    }
    private fun drawForeground(c:Canvas,x:Float,y:Float,w:Float,h:Float,a:Int){
        for(i in 0..15){val left=i%2==0;val z=.25f+(i/2)*.10f;val px=if(left)x+w*(.08f+.22f*z) else x+w*(.92f-.22f*z);val py=y+h*(.54f+.48f*z);drawPalm(c,px,py,w*(.010f+.010f*z),h*(.10f+.09f*z),a);p.color=Color.argb(a,255,44,206);c.drawRect(px+(if(left)1 else -1)*w*.018f,py-h*.11f,px+(if(left)1 else -1)*w*.035f,py-h*.035f,p)};p.shader=LinearGradient(x,y+h*.80f,x,y+h,Color.TRANSPARENT,Color.argb(a/3,255,30,190),Shader.TileMode.CLAMP);c.drawRect(x,y+h*.78f,x+w,y+h,p);p.shader=null
    }
    private fun drawPalm(c:Canvas,x:Float,y:Float,r:Float,h:Float,a:Int){
        p.color=Color.argb(a,123,67,33);p.strokeWidth=max(1f,r*.5f);c.drawLine(x,y,x+r*.25f,y-h,p);val tx=x+r*.25f;val ty=y-h;p.color=Color.argb(a,30,148,66);p.style=Paint.Style.STROKE;p.strokeWidth=max(1f,r*.35f);for(i in 0..7){val ang=-PI.toFloat()+i*PI.toFloat()/7f;c.drawLine(tx,ty,tx+cos(ang)*r*3.2f,ty+sin(ang)*r*1.6f,p)};p.style=Paint.Style.FILL
    }
    private fun drawWeather(c:Canvas,x:Float,y:Float,w:Float,h:Float,u:Float,t:Float,kind:String,a:Int){
        when(kind){"rain","storm"->{p.color=Color.argb(a/2,170,210,255);p.strokeWidth=max(1f,u);for(i in 0..35){val rx=x+w*Fx.rnd(3100L+i*67L);val ry=y+(h*Fx.rnd(4100L+i*79L)+t*h*.7f)%h;c.drawLine(rx,ry,rx-u*3f,ry+u*8f,p)}};"snow"->{p.color=Color.argb(a*3/4,240,245,255);for(i in 0..28){val rx=x+(w*Fx.rnd(6100L+i*59L)+sin(t+i)*u*5)%w;val ry=y+(h*Fx.rnd(7100L+i*89L)+t*h*.08f)%h;c.drawCircle(rx,ry,u*(1f+Fx.rnd(8100L+i*31L)),p)}};"fog"->{p.shader=LinearGradient(x,y,x+w,y,Color.TRANSPARENT,Color.argb(a/2,210,205,225),Shader.TileMode.MIRROR);c.drawRect(x,y+h*.20f,x+w,y+h*.90f,p);p.shader=null}};if(kind=="storm"&&sin(t*1.7f)>.985f){p.color=Color.argb(a/2,230,230,255);c.drawRect(x,y,x+w,y+h,p)}
    }
}
