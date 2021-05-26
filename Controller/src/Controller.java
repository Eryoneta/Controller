import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
@SuppressWarnings("serial")
public class Controller{
	private Dimension tela=Toolkit.getDefaultToolkit().getScreenSize();
	private boolean fullscreen=false;
	private int portInps=618;
	private int portImgs=1127;
	private ServerSocket serverInps;
	private ServerSocket serverImgs;
	private String IP="192.168.0.10";
	private BufferedImage moldura;
	private int modo=0;
	private JDialog janela=new JDialog(){
		{
			setSize(tela);
			setLocation(0,0);
			setAlwaysOnTop(true);
			setBackground(new Color(0,0,0));
			setUndecorated(true);
			setOpacity((float)0.01);
			setCursor(getToolkit().createCustomCursor(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),new Point(),null));
			setFocusTraversalKeysEnabled(false);
			addKeyListener(new KeyAdapter(){//DETECTA TECLA
				public void keyPressed(KeyEvent k){//AO PRESSIONAR
					if(k.isShiftDown()&&k.getKeyCode()==KeyEvent.VK_ESCAPE){//SHIFT+ESC=SAIR
						enviaInputs("KR:"+KeyEvent.VK_SHIFT);
						enviaInputs("Fechar");
						System.exit(0);
					}else if(k.isShiftDown()&&k.getKeyCode()==KeyEvent.VK_SPACE){//SHIFT+SPACE=FULLSCREEN
						fullscreen=!fullscreen;
						janela.setOpacity(fullscreen?1:(float)0.01);
						if(fullscreen)janela.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
							else janela.setCursor(janela.getToolkit().createCustomCursor(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),new Point(),null));
					}else enviaInputs("KP:"+k.getKeyCode());
				}
				public void keyReleased(KeyEvent k){//AO SOLTAR
					enviaInputs("KR:"+k.getKeyCode());
				}
			});
			addMouseWheelListener(new MouseWheelListener(){//SCROOL DO MOUSE
				public void mouseWheelMoved(MouseWheelEvent w){//SCROOL MOVIDO
					enviaInputs("MW:"+w.getWheelRotation());
				}
			});
			addMouseListener(new MouseAdapter(){//AO MOVER O MOUSE
				public void mousePressed(MouseEvent m){
					if((m.getModifiers()&InputEvent.BUTTON1_MASK)!=0)enviaInputs("MBP:"+InputEvent.BUTTON1_MASK);
						else if((m.getModifiers()&InputEvent.BUTTON2_MASK)!=0)enviaInputs("MBP:"+InputEvent.BUTTON2_MASK);
							else if((m.getModifiers()&InputEvent.BUTTON3_MASK)!=0)enviaInputs("MBP:"+InputEvent.BUTTON3_MASK);
				}
				public void mouseReleased(MouseEvent m){
					if((m.getModifiers()&InputEvent.BUTTON1_MASK)!=0)enviaInputs("MBR:"+InputEvent.BUTTON1_MASK);
						else if((m.getModifiers()&InputEvent.BUTTON2_MASK)!=0)enviaInputs("MBR:"+InputEvent.BUTTON2_MASK);
							else if((m.getModifiers()&InputEvent.BUTTON3_MASK)!=0)enviaInputs("MBR:"+InputEvent.BUTTON3_MASK);
				}
			});
			addMouseMotionListener(new MouseAdapter(){//AO MOVER O MOUSE
				public void mouseMoved(MouseEvent m){
					if(janela.isVisible())enviaInputs("MP:"+m.getX()+","+m.getY());
				}
				public void mouseDragged(MouseEvent m){
					if(janela.isVisible())enviaInputs("MP:"+m.getX()+","+m.getY());
				}
			});
		}
	};
	public static void main(String[]vars){new Controller();}
	public Controller(){
		JOptionPane.showOptionDialog(null,"Modo de comunicação?","Modo",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,new Component[]{
			new JButton("Transmissor"){{
				setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Transmissor.png"))));
				addActionListener(new ActionListener(){public void actionPerformed(ActionEvent a){modo=1;JOptionPane.getRootFrame().dispose();}});
			}},
			new JButton("Receptor"){{
				setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("Receptor.png"))));
				addActionListener(new ActionListener(){public void actionPerformed(ActionEvent a){modo=2;JOptionPane.getRootFrame().dispose();}});
			}}
		},null);
		if(modo==0)System.exit(0);//CANCELA
		else if(modo==1){//MODO TRANSMISSOR
			IP=JOptionPane.showInputDialog("IP do receptor:",IP);
			moldura=new BufferedImage(tela.width,tela.height,BufferedImage.TYPE_INT_RGB);
			mouseDetect();
			recebeImagens();
			janela.setVisible(true);
			
		}else if(modo==2){//MODO RECEPTOR
			try{JOptionPane.showMessageDialog(null,InetAddress.getLocalHost(),"IP deste transmissor",JOptionPane.INFORMATION_MESSAGE);}catch(HeadlessException|UnknownHostException erro){System.exit(0);}
			recebeInputs();
			enviaImagens();
		}
		try{
			SystemTray.getSystemTray().add(new TrayIcon(ImageIO.read(getClass().getResource((modo==1?"Transmissor":"Receptor")+".png")),modo==1?"Transmissor":"Receptor",new PopupMenu(){{
				add(new MenuItem("Sair"){{addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){System.exit(0);}});}});
			}}));
		}catch(AWTException|IOException erro){
			JOptionPane.showMessageDialog(null,"Erro ao carregar Ícone!\n"+erro,"Error",JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	private void mouseDetect(){
		new Thread(new Runnable(){
			public void run(){
				long tempoUltimoLoop=System.nanoTime();
				final long tempoOptimizado=1000000000/60;
				boolean lock=false;
				while(true){
					tempoUltimoLoop=System.nanoTime();
					final Point MousePosition=MouseInfo.getPointerInfo().getLocation();
					if(MousePosition.x==tela.width-1&&MousePosition.y==0)if(!lock){
						janela.setVisible(!janela.isVisible());
						lock=true;
						if(janela.isVisible())try{
							enviaInputs("C:"+Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor));
						}catch(HeadlessException|UnsupportedFlavorException|IOException erro){}else{
							enviaInputs("MP:"+tela.width+",0");
							enviaInputs("C?");
						}
					}else;else lock=false;
					try{Thread.sleep((tempoUltimoLoop-System.nanoTime()+tempoOptimizado)/1000000);}catch(Exception erro){}
				}
			}
		}).start();
	}
	private void enviaInputs(String dados){
		try{
			//INICIA
			Socket socket=new Socket(IP,portInps);
			ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream input=new ObjectInputStream(socket.getInputStream());
			//ENVIA
			output.writeObject(dados);
			//RECEBE
			String copy=(String)input.readObject();
			if(!copy.equals("")){
				StringSelection data=new StringSelection(copy);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data,data);
			}
			//ENCERRA
			output.close();
			input.close();
			socket.close();
		}catch(IOException|ClassNotFoundException erro){
			janela.setSize(0,0);
			janela.setVisible(true);
			JOptionPane.showMessageDialog(null,"Erro no contato de inputs!\n"+erro,"Erro!",JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	private void recebeInputs(){
		new Thread(new Runnable(){
			public void run(){
				try{
					serverInps=new ServerSocket(portInps);
					Robot Bot=null;
					try{Bot=new Robot();}catch(AWTException erro){System.exit(0);}
					Toolkit.getDefaultToolkit().setLockingKeyState(KeyEvent.VK_NUM_LOCK,false);
					while(true){
						//INICIA
						Socket socket=serverInps.accept();
						ObjectInputStream input=new ObjectInputStream(socket.getInputStream());
						ObjectOutputStream output=new ObjectOutputStream(socket.getOutputStream());
						//RECEBE
						String dados=(String)input.readObject();
						int tam=(dados.equals("Fechar")?0:dados.length());
						if(dados.startsWith("MP:")){
							int x=Integer.parseInt(dados.substring(3,dados.indexOf(",")));
							int y=Integer.parseInt(dados.substring(dados.indexOf(",")+1,tam));
							Bot.mouseMove(x,y);
						}else if(dados.startsWith("MBP:")){//MOUSE BUTTON PRESS
							Bot.mousePress(Integer.parseInt(dados.substring(4,tam)));
						}else if(dados.startsWith("MBR:")){//MOUSE BUTTON RELEASE
							Bot.mouseRelease(Integer.parseInt(dados.substring(4,tam)));
						}else if(dados.startsWith("MW:")){//MOUSE WHEEL
							Bot.mouseWheel(Integer.parseInt(dados.substring(3,tam)));
						}else if(dados.startsWith("KP:")){//KEY PRESS
							Bot.keyPress(Integer.parseInt(dados.substring(3,tam)));
						}else if(dados.startsWith("KR:")){//KEY RELEASE
							Bot.keyRelease(Integer.parseInt(dados.substring(3,tam)));
						}else if(dados.startsWith("C:")){//COLAR
							StringSelection data=new StringSelection(dados.substring(2,tam));
							Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data,data);
						}
						if(dados.equals("C?")){//COPIAR
							//ENVIA
							try{
								output.writeObject(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor));
							}catch(HeadlessException|UnsupportedFlavorException erro){output.writeObject("");}
						}else output.writeObject("");
						//ENCERRA
						input.close();
						output.close();
						socket.close();
						if(dados.equals("Fechar"))break;
					}
				}catch(IOException|ClassNotFoundException erro){
					JOptionPane.showMessageDialog(null,"Erro no contato de inputs!\n"+erro,"Erro!",JOptionPane.ERROR_MESSAGE);
				}finally{
					try{serverInps.close();serverImgs.close();}catch(IOException error){}
					System.exit(0);
				}
			}
		}).start();
	}
	private void enviaImagens(){
		new Thread(new Runnable(){
			public void run(){
				try{
					serverImgs=new ServerSocket(portImgs);
					Robot Bot=null;
					try{Bot=new Robot();}catch(AWTException erro){System.exit(0);}
					while(true){
						//INICIA
						Socket socket=serverImgs.accept();
						OutputStream output=socket.getOutputStream();
						//ENVIA
						ImageIO.write(Bot.createScreenCapture(new Rectangle(0,0,tela.width,tela.height)),"jpg",output);
						output.flush();
						//ENCERRA
						output.close();
						socket.close();
					}
				}catch(IOException erro){
					JOptionPane.showMessageDialog(null,"Erro no contato de imagens!\n"+erro,"Erro!",JOptionPane.ERROR_MESSAGE);
					try{serverInps.close();serverImgs.close();}catch(IOException error){}
					System.exit(0);
				}
			}
		}).start();
	}
	private void recebeImagens(){
		new Thread(new Runnable(){
			public void run(){
				long tempoUltimoLoop=System.nanoTime();
				long tempoOptimizado=1000000000/60;
				while(true){
					tempoUltimoLoop=System.nanoTime();
					if(janela.isVisible()&&fullscreen)try{
						//INICIA
						Socket socket=new Socket(IP,portImgs);
						InputStream input=socket.getInputStream();
						//RECEBE
						BufferedImage image=ImageIO.read(ImageIO.createImageInputStream(input));
						Graphics quadro=moldura.getGraphics();
						quadro.drawImage(image,0,0,null);
						janela.getGraphics().drawImage(moldura,0,0,janela);//DESENHA
						//ENCERRA
						input.close();
						socket.close();
					}catch(IOException erro){
						janela.setSize(0,0);
						janela.setVisible(true);
						JOptionPane.showMessageDialog(null,"Erro no contato de imagens!\n"+erro,"Erro!",JOptionPane.ERROR_MESSAGE);
						System.exit(0);
					}
					try{Thread.sleep((tempoUltimoLoop-System.nanoTime()+tempoOptimizado)/1000000);}catch(Exception erro){}
				}
			}
		}).start();
	}
}