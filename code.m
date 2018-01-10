% concat the data from both users into a single matrix
uu =  cat(1,user1input,user2input,user1Secondtimeinput,user2Secondtimeinput);

% do a random permutation for the samples to add some randomness
uu2 = uu(randperm(size(uu,1),size(uu,1)),:);

% take out the parameters data (samples) without the labels
uudata = uu2(:,1:16);

% take out the samples labels 
uutarget = uu2(:,end);

% construct the labels matrix in a way that the neural network will
% understand
for i = 1 : size(uu2,1)
ttar(i,uutarget(i)) = 1;
end

% construct a neural network with two layers each have 20 neurons
%net=patternnet([20 20 20]);
net=patternnet([20 20]);

% set the transfer function in each layer to be sigmoid
net.layers{1}.transferFcn='logsig';
net.layers{2}.transferFcn='logsig';
%net.layers{3}.transferFcn='logsig';
% set the performance function to be crossentropy
net.performFcn='crossentropy';

% set the training algorithm to gradient descent
net.trainFcn='traingd';

% set the learning rate to be 0.1
net.trainParam.lr=0.08;

% for randomness
rng('shuffle')

% generate 108 random numbers each represent the fold number that each
% sample in the dataset will fall in
r = randi(5,1,size(uu2,1));
indices = r';

% the k fold iterations, in each iteration we take 4 chunks for training
% the neural network and 1 chunk for testing then we report the validation
% accuracy and the training time. at the end we calculate the average
% accuracy 
for i=1:5
    testInd=(indices==i);
    trainInd=~testInd;
    trainData=uudata(trainInd,:)';
    trainLabel=ttar(trainInd,:)';
    testData=uudata(testInd,:)';
    testLabel=ttar(testInd,:)';
    tic;
    net=train(net,trainData,trainLabel);
    time(i)=toc;
    avg_time=mean(time);
    y=net(testData);
    output=vec2ind(y);
    acc(i)=100*sum(output==vec2ind(testLabel))/size(output,2);
    avg_acc=mean(acc);
end
