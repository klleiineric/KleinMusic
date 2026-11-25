# Compilar o motor de anúncios no GitHub (passo a passo)

1. Crie um repositório (pode ser privado) chamado KleinMusic em github.com.
2. Envie o conteúdo da pasta KleinMusic, mantendo a estrutura de pastas.
   Atenção: o upload de pastas pelo navegador do celular costuma falhar.
   Alternativa confiável: Termux com git (git clone, git add, git commit, git push).
3. No repositório, abra a aba Actions, escolha "Compilar motor de anúncios" e toque em Run workflow.
4. Quando terminar (verde), abra a execução e baixe o artefato "jniLibs".
5. Os arquivos libklein_adblock.so ficam em app/src/main/jniLibs/<processador>/.
   Se o workflow falhar (vermelho), copie a mensagem de erro e me envie.
